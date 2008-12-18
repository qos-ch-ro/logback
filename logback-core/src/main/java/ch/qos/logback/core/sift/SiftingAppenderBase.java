/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 2000-2008, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.core.sift;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * This appender can contains other appenders which it can build dynamically
 * depending on MDC values. The built appender is specified as part of a
 * configuration file.
 * 
 * <p>See the logback manual for further details.
 * 
 * 
 * @author Ceki Gulcu
 */
public abstract class SiftingAppenderBase<E, K> extends
    UnsynchronizedAppenderBase<E> {

  protected AppenderTracker<E, K> appenderTracker = new AppenderTrackerImpl<E, K>();
  // Map<String, Appender<LoggingEvent>> appenderMap = new Hashtable<String,
  // Appender<LoggingEvent>>();

  String mdcKey;
  String defaultValue;

  AppenderFactoryBase<E, K> appenderFactory;

  public void setAppenderFactory(AppenderFactoryBase<E, K> appenderFactory) {
    this.appenderFactory = appenderFactory;
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void stop() {
    for (Appender<E> appender : appenderTracker.valueList()) {
      appender.stop();
    }
  }

  abstract protected K getDiscriminatingValue(E event);

  abstract protected long getTimestamp(E event);

  @Override
  protected void append(E event) {
    if (!isStarted()) {
      return;
    }

    K value = getDiscriminatingValue(event);
    long timestamp = getTimestamp(event);

    Appender<E> appender = appenderTracker.get(value, timestamp);

    if (appender == null) {
      try {
        appender = appenderFactory.buildAppender(context, value);
        if (appender != null) {
          appenderTracker.put(value, appender, timestamp);
        }
      } catch (JoranException e) {
        addError("Failed to build appender for [" + value + "]", e);
        return;
      }
    }
    appenderTracker.stopStaleAppenders(timestamp);
    appender.doAppend(event);
  }

  public String getMdcKey() {
    return mdcKey;
  }

  public void setMdcKey(String mdcKey) {
    this.mdcKey = mdcKey;
  }

  /**
   * @see #setDefaultValue(String)
   * @return
   */
  public String getDefaultValue() {
    return defaultValue;
  }

}