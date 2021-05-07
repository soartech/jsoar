package org.jsoar.debugger.syntax;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.awt.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TextStyle {

  private String styleType;
  private boolean bold = false;
  private boolean underline = false;
  private boolean strikethrough = false;
  private boolean italic = false;
  private int fontSize = 0;
  private boolean enabled = true;

  private Color foreground = Color.WHITE;
  private Color background = Color.BLACK;

  public TextStyle() {}

  public TextStyle(TextStyle copy) {
    this.bold = copy.bold;
    this.underline = copy.underline;
    this.strikethrough = copy.strikethrough;
    this.italic = copy.italic;
    this.fontSize = copy.fontSize;
    this.foreground = copy.foreground;
    this.background = copy.background;
    this.styleType = copy.styleType;
  }

  @JsonIgnore
  public AttributeSet getAttributes() {
    SimpleAttributeSet attrs = new SimpleAttributeSet();

    StyleConstants.setBold(attrs, bold);
    StyleConstants.setUnderline(attrs, underline);
    StyleConstants.setStrikeThrough(attrs, strikethrough);
    StyleConstants.setItalic(attrs, italic);
    if (fontSize > 0) {
      StyleConstants.setFontSize(attrs, fontSize);
    }
    StyleConstants.setForeground(attrs, foreground);
    StyleConstants.setBackground(attrs, background);

    return attrs;
  }

  public String getStyleType() {
    return styleType;
  }

  public void setStyleType(String styleType) {
    this.styleType = styleType;
  }

  public boolean isBold() {
    return bold;
  }

  public void setBold(boolean bold) {
    this.bold = bold;
  }

  public boolean isUnderline() {
    return underline;
  }

  public void setUnderline(boolean underline) {
    this.underline = underline;
  }

  public boolean isStrikethrough() {
    return strikethrough;
  }

  public void setStrikethrough(boolean strikethrough) {
    this.strikethrough = strikethrough;
  }

  public boolean isItalic() {
    return italic;
  }

  public void setItalic(boolean italic) {
    this.italic = italic;
  }

  public int getFontSize() {
    return fontSize;
  }

  public void setFontSize(int fontSize) {
    this.fontSize = fontSize;
  }

  @JsonIgnore
  public Color getForeground() {
    return foreground;
  }

  public void setForeground(Color foreground) {
    this.foreground = foreground;
  }

  @JsonIgnore
  public Color getBackground() {
    return background;
  }

  public void setBackground(Color background) {
    this.background = background;
  }

  public void setForegroundRgb(float[] components) {
    foreground = new Color(components[0], components[1], components[2], components[3]);
  }

  public float[] getForegroundRgb() {
    return foreground.getRGBComponents(null);
  }

  public void setBackgroundRgb(float[] components) {
    background = new Color(components[0], components[1], components[2], components[3]);
  }

  public float[] getBackgroundRgb() {
    return background.getRGBComponents(null);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
