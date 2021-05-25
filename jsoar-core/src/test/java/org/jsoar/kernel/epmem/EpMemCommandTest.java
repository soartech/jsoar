package org.jsoar.kernel.epmem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.AppendDatabaseChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GraphMatchChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.LazyCommitChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Learning;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.PageChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Trigger;
import org.jsoar.kernel.epmem.EpmemCommand.EpmemC;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;
import org.junit.Test;

public class EpMemCommandTest {

  @Test
  public void testBackup() throws SQLException {
    Agent agent = mock(Agent.class);
    Printer printer = mock(Printer.class);
    when(agent.getPrinter()).thenReturn(printer);
    DefaultEpisodicMemory episodicMemory = mock(DefaultEpisodicMemory.class);
    when(episodicMemory.epmem_backup_db(eq("FILE_ONE FILE_TWO"), any())).thenReturn(true);
    when(agent.getAdapter(DefaultEpisodicMemory.class)).thenReturn(episodicMemory);

    SymbolFactoryImpl symbols = mock(SymbolFactoryImpl.class);
    when(agent.getAdapter(SymbolFactoryImpl.class)).thenReturn(symbols);

    EpmemC executable;

    // Given a EpMemCommand
    EpmemCommand command = new EpmemCommand(agent);
    executable = (EpmemC) command.getCommand();
    // And backup file names specified
    executable.backupFileName = new String[] {"FILE_ONE", "FILE_TWO"};

    // When executing command
    executable.run();

    // Then episodic memory is back-up to specified files
    verify(episodicMemory, times(1)).epmem_backup_db(eq("FILE_ONE FILE_TWO"), any());
    // And 'EpMem| Database backed up to .*' is printed
    verify(printer, times(1)).print(matches("EpMem| Database backed up to .*"));
  }

  @Test
  public void testSetParameterLearning() {
    setParameterValue("learning", DefaultEpisodicMemoryParams.LEARNING, Learning.off);
  }

  @Test
  public void testSetParameterTrigger() {
    setParameterValue("trigger", DefaultEpisodicMemoryParams.TRIGGER, Trigger.output);
  }

  @Test
  public void testSetParameterPhase() {
    setParameterValue("phase", DefaultEpisodicMemoryParams.PHASE, Phase.selection);
  }

  @Test
  public void testSetParameterGraphMatch() {
    setParameterValue("graph-match", DefaultEpisodicMemoryParams.GRAPH_MATCH, GraphMatchChoices.on);
  }

  @Test
  public void testSetParameterBalance() {
    setParameterValue("balance", DefaultEpisodicMemoryParams.BALANCE, 2.0);
  }

  @Test
  public void testSetParameterOptimization() {
    setParameterValue(
        "optimization", DefaultEpisodicMemoryParams.OPTIMIZATION, Optimization.performance);
  }

  @Test
  public void testSetParameterPath() {
    setParameterValue("path", DefaultEpisodicMemoryParams.PATH, "TEST");
  }

  @Test
  public void testSetParameterAppendDatabase() {
    setParameterValue(
        "append-database", DefaultEpisodicMemoryParams.APPEND_DB, AppendDatabaseChoices.off);
  }

  @Test
  public void testSetGraphMatchOrdering() {
    setParameterValue(
        "graph-match-ordering", DefaultEpisodicMemoryParams.GM_ORDERING, GmOrderingChoices.mcv);
  }

  @Test
  public void testSetParameterPageSize() {
    setParameterValue(
        "page-size",
        DefaultEpisodicMemoryParams.PAGE_SIZE,
        PageChoices.page_1k.name(),
        PageChoices.page_1k);
  }

  @Test
  public void testSetParameterCacheSize() {
    setParameterValue("cache-size", DefaultEpisodicMemoryParams.CACHE_SIZE, 15l);
  }

  @Test
  public void testSetParameterLazyCommit() {
    setParameterValue("lazy-commit", DefaultEpisodicMemoryParams.LAZY_COMMIT, LazyCommitChoices.on);
  }

  private void setParameterValue(
      String propertyName, PropertyKey propertyKey, Object propertyValue) {
    setParameterValue(propertyName, propertyKey, propertyValue, propertyValue);
  }

  private void setParameterValue(
      String propertyName,
      PropertyKey propertyKey,
      Object propertyValue,
      Object expectedPropertyValue) {
    Agent agent = mock(Agent.class);
    Printer printer = mock(Printer.class);
    when(agent.getPrinter()).thenReturn(printer);
    DefaultEpisodicMemory episodicMemory = mock(DefaultEpisodicMemory.class);
    when(agent.getAdapter(DefaultEpisodicMemory.class)).thenReturn(episodicMemory);
    DefaultEpisodicMemoryParams episodicMemoryParams = mock(DefaultEpisodicMemoryParams.class);
    when(episodicMemory.getParams()).thenReturn(episodicMemoryParams);
    PropertyManager propertyManager = mock(PropertyManager.class);
    when(episodicMemoryParams.getProperties()).thenReturn(propertyManager);

    SymbolFactoryImpl symbols = mock(SymbolFactoryImpl.class);
    when(agent.getAdapter(SymbolFactoryImpl.class)).thenReturn(symbols);

    EpmemC executable;

    // Given a EpMemCommand
    EpmemCommand command = new EpmemCommand(agent);
    executable = (EpmemC) command.getCommand();
    // And name of parameter to set is specified
    executable.setParam = propertyName;
    // And value of parameter is specified
    executable.param = propertyValue.toString();

    // When executing command
    executable.run();

    // Then property learning is set to specified value
    verify(propertyManager, times(1)).set(propertyKey, expectedPropertyValue);
    // And 'Set learning to .*' is printed
    verify(printer, times(1)).print(matches("Set .* to .*"));
  }
}
