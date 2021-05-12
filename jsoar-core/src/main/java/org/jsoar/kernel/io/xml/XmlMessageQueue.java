/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 3, 2009
 */
package org.jsoar.kernel.io.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.w3c.dom.Element;

/**
 * An input component that manages a queue of messages on the input-link. Messages are decoded from
 * XML using an instance of {@link XmlToWme}.
 *
 * <p>A typical message queue (defaults) would look like this:
 *
 * <pre>
 * ^io.input-link
 *   ^messages
 *     ^message
 *       ^/next  (points to id of next message in queue)
 *       ... structure of message from XmlToWme ...
 *     ^message
 *       ^/previous (points to id of previous message)
 *       ^/next  (points to id of next message in queue)
 *       ... structure of message from XmlToWme ...
 *     ^message
 *       ^/previous (points to id of previous message)
 *       ... structure of message from XmlToWme ... *
 * </pre>
 *
 * <p>The newest message in the queue will have no {@code ^/next} WME. The oldest will have no
 * {@code ^/previous} WME.
 *
 * <p>Instances of this class are configured and created using a builder:
 *
 * <pre>{@code
 * XmlMessageQueue queue = XmlMessageQueueu.newBuilder(agent.getInputOutput()).
 *      timeToLive(100).               // messages stay for 100 decision cycles
 *      queueName("contact-reports").  // set the attribute of the root WME of the queue
 *      converter(...).                // set the XmlToWme converter to use
 *      create();
 *
 * }</pre>
 *
 * <p>New messages can be added to the queue <b>from any thread</b> with the {@link #add(Element)}
 * method:
 *
 * <pre>{@code
 * queue.add(XmlTools.parse("<test>this is a test message</test>").getDocumentElement());
 * }</pre>
 *
 * <p>Using the defaults, this would construct input like this:
 *
 * <pre>
 * ^io.input-link
 *   ^messages
 *     ^test
 *       ^/text |this is a test message|
 * </pre>
 *
 * <p>Once added, a message will remain in working memory until {@link Builder#timeToLive()}
 * decision cycles have passed. Note that when removed, only the root WME and {@code /next} and
 * {@code /previous} WMEs of a message are removed so any o-supported pointers to the message (or
 * its substructure) will keep it in working memory as long as they remain.
 *
 * <p>To remove the queue from the input-link complete, use the {@link #dispose()} method.
 *
 * @author ray
 */
public class XmlMessageQueue {
  private final InputOutput io;

  private final XmlToWme converter;
  private final long timeToLive;
  private final String queueName;

  private final Listener listener = new Listener();
  private final ConcurrentLinkedQueue<NewMessage> newMessages =
      new ConcurrentLinkedQueue<NewMessage>();
  private final LinkedList<Entry> entries = new LinkedList<Entry>();

  /** the root WME of the queue, created on first input event */
  private final AtomicReference<InputWme> queueWme = new AtomicReference<InputWme>();

  /**
   * Builder class for {@link XmlMessageQueue}. Instantiate with {@link
   * XmlMessageQueue#newBuilder(InputOutput)}
   */
  public static class Builder {
    private final InputOutput io;
    private XmlToWme converter;
    private long timeToLive = 50;
    private String queueName = "messages";

    private Builder(@NonNull InputOutput io) {
      this.io = io;
    }

    public XmlToWme converter() {
      return converter;
    }
    /**
     * Set the {@link XmlToWme} converter used by the queue. Defaults to {@link DefaultXmlToWme}.
     *
     * @param c the converter
     * @return this
     */
    public Builder converter(XmlToWme c) {
      this.converter = c;
      return this;
    }

    public long timeToLive() {
      return timeToLive;
    }
    /**
     * Set the number of input cycles that messages stay in working memory before being removed.
     *
     * @param ttl number of input cycles
     * @return this
     */
    public Builder timeToLive(long ttl) {
      this.timeToLive = ttl;
      return this;
    }

    public String queueName() {
      return queueName;
    }
    /**
     * Set the name of the queue attribute on the input-link. Defaults to {@code "messages"}.
     *
     * @param qn the name of the queue attribute
     * @return this
     */
    public Builder queueName(String qn) {
      this.queueName = qn;
      return this;
    }

    /** @return the new message queue object with the current settings */
    public XmlMessageQueue create() {
      return new XmlMessageQueue(this);
    }
  }

  /**
   * Returns a new builder object
   *
   * @param io the I/O interface to use
   * @return a new builder object
   */
  public static Builder newBuilder(InputOutput io) {
    return new Builder(io);
  }

  private XmlMessageQueue(@NonNull Builder builder) {
    this.io = builder.io;
    this.converter = builder.converter != null ? builder.converter : DefaultXmlToWme.forInput(io);
    this.queueName = builder.queueName;
    this.timeToLive = builder.timeToLive;

    this.io.getEvents().addListener(InputEvent.class, listener);
    this.io.getEvents().addListener(BeforeInitSoarEvent.class, listener);
  }

  /**
   * Dispose this object, detaching it from the agent and removing any working memory structures it
   * may have created. WMEs will not be removed until the next input phase.
   *
   * @return this
   */
  public XmlMessageQueue dispose() {
    this.io.getEvents().removeListener(null, listener);

    final InputWme wme = queueWme.getAndSet(null);
    if (wme != null) {
      wme.remove();
    }
    newMessages.clear();
    entries.clear();

    return this;
  }

  /**
   * Add a message to the end of this queue. This method will take the given XML element and cause
   * it to be placed in working memory during the next input phase.
   *
   * <p>This method is thread-safe and will wake the agent if it is currently waiting for input.
   *
   * @param message the message
   * @return this
   */
  public XmlMessageQueue add(Element message) {
    newMessages.add(new NewMessage(message, message.getTagName()));
    io.asynchronousInputReady();

    return this;
  }

  private void update(InputEvent inputEvent) {
    removeExpiredMessages();

    createQueueWme();

    processNewMessages();
  }

  private void processNewMessages() {
    NewMessage nm = newMessages.poll();
    while (nm != null) {
      addNewMessage(nm);
      nm = newMessages.poll();
    }
  }

  private void removeExpiredMessages() {
    final Iterator<Entry> it = entries.iterator();
    while (it.hasNext()) {
      final Entry entry = it.next();
      if (entry.removeIfExpired()) {
        it.remove();
      }
    }
  }

  private void createQueueWme() {
    if (queueWme.get() == null) {
      queueWme.set(InputWmes.add(io, queueName, Symbols.NEW_ID));
    }
  }

  private Entry getLastEntry() {
    return entries.isEmpty() ? null : entries.getLast();
  }

  private void addNewMessage(NewMessage nm) {
    final Identifier queueId = queueWme.get().getValue().asIdentifier();
    final Identifier messageId = converter.fromXml(nm.element);
    final InputWme messageWme = InputWmes.add(io, queueId, nm.name, messageId);
    final Entry lastMessage = getLastEntry();
    final List<InputWme> support = new ArrayList<InputWme>();
    if (lastMessage != null) {
      support.add(InputWmes.add(io, lastMessage.getId(), "/next", messageId));
      lastMessage.supportWmes.add(InputWmes.add(io, messageId, "/previous", lastMessage.getId()));
    }

    entries.add(new Entry(messageWme, support, timeToLive));
  }

  private void beforeInitSoar(BeforeInitSoarEvent event) {
    entries.clear();
    if (!newMessages.isEmpty()) {
      io.asynchronousInputReady();
    }
  }

  private static class NewMessage {
    final Element element;
    final String name;

    public NewMessage(Element element, String name) {
      this.element = element;
      this.name = name;
    }
  }

  private static class Entry {
    final InputWme wme;
    final List<InputWme> supportWmes;
    long ttl = 50;

    public Entry(InputWme wme, List<InputWme> supportWmes, long ttl) {
      this.wme = wme;
      this.supportWmes = supportWmes;
      this.ttl = ttl;
    }

    public Identifier getId() {
      return wme.getValue().asIdentifier();
    }

    public boolean removeIfExpired() {
      ttl--;
      boolean remove = ttl == 0;
      if (remove) {
        wme.remove();
        for (InputWme wme : supportWmes) {
          wme.remove();
        }
      }
      return remove;
    }
  }

  private class Listener implements SoarEventListener {

    /* (non-Javadoc)
     * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event) {
      if (event instanceof InputEvent) {
        update((InputEvent) event);
      } else if (event instanceof BeforeInitSoarEvent) {
        beforeInitSoar((BeforeInitSoarEvent) event);
      }
    }
  }
}
