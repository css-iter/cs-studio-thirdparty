/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.common.engine.model;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.engine.Activator;
import org.csstudio.archive.common.engine.types.ArchiveEngineTypeSupport;
import org.csstudio.archive.common.service.ArchiveConnectionException;
import org.csstudio.archive.common.service.ArchiveServiceException;
import org.csstudio.archive.common.service.IArchiveEngineConfigService;
import org.csstudio.archive.common.service.archivermgmt.ArchiverMgmtEntry;
import org.csstudio.archive.common.service.channel.IArchiveChannel;
import org.csstudio.archive.common.service.channelgroup.IArchiveChannelGroup;
import org.csstudio.archive.common.service.engine.IArchiveEngine;
import org.csstudio.domain.desy.types.ITimedCssAlarmValueType;
import org.csstudio.domain.desy.types.TypeSupportException;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.service.osgi.OsgiServiceUnavailableException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import com.google.common.base.Joiner;
import com.google.common.collect.MapMaker;

/** Data model of the archive engine.
 *  @author Kay Kasemir
 *  @author Bastian Knerr
 */
public final class EngineModel {
    private static final Logger LOG =
        CentralLogger.getInstance().getLogger(EngineModel.class);

    /** Version code. See also webroot/version.html */
    public static String VERSION = "1.2.3"; //$NON-NLS-1$

    /** Name of this model */
    private String _name = "DESY Archive Engine";  //$NON-NLS-1$

    /** Thread that writes to the <code>archive</code> */
    final private WriteExecutor _writeExecutor;

    /**
     * All channels
     */
    final ConcurrentMap<String, ArchiveChannel<?, ?>> _channelMap;

    /** Groups of archived channels
     *  <p>
     *  @see channels about thread safety
     */
    final ConcurrentMap<String, ArchiveGroup> _groupMap;

//    /** Scanner for scanned channels */
//    final Scanner scanner = new Scanner();
//
//    /** Thread that runs the scanner */
//    final ScanThread scan_thread = new ScanThread(scanner);

    /** Engine states */
    public enum State {
        /** Initial model state before <code>start()</code> */
        IDLE,
        /** Running model, state after <code>start()</code> */
        RUNNING,
        /** State after <code>requestStop()</code>; still running. */
        SHUTDOWN_REQUESTED,
        /** State after <code>requestRestart()</code>; still running. */
        RESTART_REQUESTED,
        /** State while in <code>stop()</code>; will then be IDLE again. */
        STOPPING
    }

    /** Engine state */
    private State state = State.IDLE;

    /** Start time of the model */
    private ITimestamp start_time = null;

    /** Write period in seconds */
    private long _writePeriod = 30;

    /** Maximum number of repeat counts for scanned channels */
    private int max_repeats = 60;

    /** Write batch size */
    private int batch_size = 500;

    private IArchiveEngine _engine;

    /**
     * Construct model that writes to archive
     */
    public EngineModel()
        throws OsgiServiceUnavailableException, ArchiveConnectionException {

        _groupMap = new MapMaker().concurrencyLevel(2).makeMap();
        _channelMap = new MapMaker().concurrencyLevel(2).makeMap();

        applyPreferences();

        _writeExecutor = new WriteExecutor();
    }

    /** Read preference settings */
    @SuppressWarnings("nls")
    private void applyPreferences() {
        final IPreferencesService prefs = Platform.getPreferencesService();
        if (prefs == null) {
            return;
        }
        _writePeriod = prefs.getLong(Activator.PLUGIN_ID, "write_period", _writePeriod, null);
        max_repeats = prefs.getInt(Activator.PLUGIN_ID, "max_repeats", max_repeats, null);
        batch_size = prefs.getInt(Activator.PLUGIN_ID, "batch_size", batch_size, null);
        //buffer_reserve = prefs.getDouble(Activator.PLUGIN_ID, "buffer_reserve", buffer_reserve, null);
    }

    /** @return Name (description) */
    public String getName() {
        return _name;
    }

    /** @return Seconds into the future that should be ignored */
    public static long getIgnoredFutureSeconds() {
        // TODO make configurable
        // 1 day
        return 24*60*60;
    }

    /** @return Write period in seconds */
    public long getWritePeriod() {
        return _writePeriod;
    }

    /** @return Write batch size */
    public int getBatchSize() {
        return batch_size;
    }

    /** @return Current model state */
    public State getState()
    {
        return state;
    }

    /** @return Start time of the engine or <code>null</code> if not running */
    public ITimestamp getStartTime()
    {
        return start_time;
    }

    /**
     *  Add new group if not already exists.
     *
     *  @param _name Name of the group to find or add.
     *  @return ArchiveGroup
     */
    private final ArchiveGroup addGroup(final IArchiveChannelGroup groupCfg) {
        final String groupName = groupCfg.getName();
        _groupMap.putIfAbsent(groupName, new ArchiveGroup(groupName, groupCfg.getId().longValue()));
        return _groupMap.get(groupName);
    }

    /** @return Number of groups */
    public int getGroupCount()
    {
        return _groupMap.size();
    }

    /** @return Group by that name or <code>null</code> if not found */
    public ArchiveGroup getGroup(final String name)
    {
        return _groupMap.get(name);
    }

    /** @return Number of channels */
    public int getChannelCount() {
        return _channelMap.size();
    }

    /** @return Channel by that name or <code>null</code> if not found */
    public ArchiveChannel<?, ?> getChannel(final String name) {
        return _channelMap.get(name);
    }

    /** @return Channel by that name or <code>null</code> if not found */
    public Collection<ArchiveChannel<?, ?>> getChannels() {
        return _channelMap.values();
    }




    /** Start processing all channels and writing to archive. */
    public void start() throws Exception {

        start_time = TimestampFactory.now();
        state = State.RUNNING;
        _writeExecutor.start(_writePeriod);

        for (final ArchiveGroup group : _groupMap.values()) {
            group.start(_engine.getId(),
                        ArchiverMgmtEntry.ARCHIVER_START);
            // Check for stop request.
            // Unfortunately, we don't check inside group.start(),
            // which could have run for some time....
            if (state == State.SHUTDOWN_REQUESTED) {
                break;
            }
        }
        //scan_thread.start();
    }

    /** @return Timestamp of end of last write run */
    public ITimestamp getLastWriteTime() {
        return _writeExecutor.getLastWriteTime();
    }

    /** @return Average number of values per write run */
    public double getAvgWriteCount() {
        return _writeExecutor.getAvgWriteCount();
    }

    /** @return  Average duration of write run in seconds */
    public double getAvgWriteDuration() {
        return _writeExecutor.getAvgWriteDuration();
    }

    /** Ask the model to stop.
     *  Merely updates the model state.
     *  @see #getState()
     */
    public void requestStop() {
        state = State.SHUTDOWN_REQUESTED;
    }

    /** Ask the model to restart.
     *  Merely updates the model state.
     *  @see #getState()
     */
    public void requestRestart() {
        state = State.RESTART_REQUESTED;
    }

    /** Reset engine statistics */
    public void reset() {
        _writeExecutor.reset();
        synchronized (this) {
            for (final ArchiveChannel<?, ?> channel : _channelMap.values()) {
                channel.reset();
            }
        }
    }

    /** Stop monitoring the channels, flush the write buffers. */
    @SuppressWarnings("nls")
    public void stop() throws Exception {
        state = State.STOPPING;
        //LOG.info("Stopping scanner");
        // Stop scanning
        //scan_thread.stop();
        // Assert that scanning has stopped before we add 'off' events
        //scan_thread.join();
        // Disconnect from network
        LOG.info("Stopping archive groups");
        for (final ArchiveGroup group : _groupMap.values()) {
            group.stop(_engine.getId(), ArchiverMgmtEntry.ARCHIVER_STOP);
        }

        LOG.info("Shutting down writer");
        _writeExecutor.shutdown();

        // Close the engine config connection
        // Activator.getDefault().getArchiveEngineConfigService().disconnect();

        // Update state
        state = State.IDLE;
        start_time = null;
    }


    /** Read configuration of model from RDB.
     *  @param p_name Name of engine in RDB
     *  @param port Current HTTPD port
     * @throws ArchiveReadConfigException
     */
    @SuppressWarnings("nls")
    public void readConfig(@Nonnull final String engineName, final int port) throws ArchiveReadConfigException {
        try {
            if (state != State.IDLE) {
                LOG.error("Read configuration while state " + state + ". Should be " + State.IDLE);
                return;
            }
            _name = engineName;

            final IArchiveEngineConfigService configService = Activator.getDefault().getArchiveEngineConfigService();

            _engine = configService.findEngine(_name);
            if (_engine == null) {
                LOG.error("Unknown engine '" + _name + "'.");
                return;
            }
            // Is the configuration consistent?
            if (_engine.getUrl().getPort() != port) {
                LOG.error("Engine " + _name + " running on port " + port +
                          " while configuration requires " + _engine.getUrl().toString());
                return;
            }

            final Collection<IArchiveChannelGroup> groups = configService.getGroupsForEngine(_engine.getId());

            for (final IArchiveChannelGroup groupCfg : groups) {
                configureGroup(configService, groupCfg);
            }
        } catch (final Exception e) {
            handleExceptions(e);
        }
    }

    private void configureGroup(@Nonnull final IArchiveEngineConfigService configService,
                                @Nonnull final IArchiveChannelGroup groupCfg) throws ArchiveServiceException,
                                                                                     TypeSupportException {
        final ArchiveGroup group = addGroup(groupCfg);
        // Add channels to group
        final Collection<IArchiveChannel> channelCfgs =
            configService.getChannelsByGroupId(groupCfg.getId());

        for (final IArchiveChannel channelCfg : channelCfgs) {

            final ArchiveChannel<Object, ITimedCssAlarmValueType<Object>> channel =
                ArchiveEngineTypeSupport.toArchiveChannel(channelCfg);

            _writeExecutor.addChannel(channel);

            _channelMap.putIfAbsent(channel.getName(), channel);
            group.add(channel);
        }
    }

    private void handleExceptions(@Nonnull final Exception inE) throws ArchiveReadConfigException {
        final String msg = "Failure during archive engine configuration retrieval: ";
        try {
            throw inE;
        } catch (final OsgiServiceUnavailableException e) {
            throw new ArchiveReadConfigException(msg + "Service unavailable.", e);
        } catch (final ArchiveServiceException e) {
            throw new ArchiveReadConfigException(msg + "Internal service exception.", e);
        } catch (final MalformedURLException e) {
            throw new ArchiveReadConfigException(msg + "Engine url malformed.", e);
        } catch (final TypeSupportException e) {
            throw new ArchiveReadConfigException(msg + "Channel type not supported.", e);
        } catch (final Exception re) {
            throw new RuntimeException(re);
        }
    }

    /** Remove all channels and groups. */
    @SuppressWarnings("nls")
    public void clearConfig() {
        if (state != State.IDLE) {
            throw new IllegalStateException("Only allowed in IDLE state");
        }
        _groupMap.clear();
        _channelMap.clear();
        //scanner.clear();
    }

    /** Write debug info to stdout */
    @SuppressWarnings("nls")
    public void dumpDebugInfo() {
        System.out.println(TimestampFactory.now().toString() + ": Debug info");
        for (final ArchiveChannel<?, ?> channel : _channelMap.values()) {
            final StringBuilder buf = new StringBuilder();
            buf.append("'" + channel.getName() + "' (");
            buf.append(Joiner.on(",").join(channel.getGroups()));
            buf.append("): ");
            buf.append(channel.getMechanism());

            buf.append(channel.isEnabled() ? ", enabled" : ", DISABLED");
            buf.append(channel.isConnected() ? ", connected (" : ", DISCONNECTED (");
            buf.append(channel.getInternalState() + ")");
            buf.append(", value " + channel.getCurrentValue());
            buf.append(", last stored " + channel.getLastArchivedValue());
            System.out.println(buf.toString());
        }
    }

    @Nonnull
    public Collection<ArchiveGroup> getGroups() {
        return _groupMap.values();
    }


//  /** Add a channel to the engine under given group.
//   *  @param channelName Channel name
//   *  @param group Name of the group to which to add
//   *  @param enablement How channel acts on the group
//   *  @param monitor Monitor or scan?
//   *  @param sample_val Sample mode configuration value: 'delta' for monitor
//   *  @param period Estimated update period [seconds]
//   *  @return {@link ArchiveChannel}
//   *  @throws Exception on error from channel creation
//   */
//  @SuppressWarnings("nls")
//  public <T> ArchiveChannel<T> addChannel(final String channelName,
//                                            final ArchiveGroup group,
//                                            final Enablement enablement,
//                                            final boolean monitor,
//                                            final double sample_val,
//                                            final double period) throws Exception
//  {
//      if (state != State.IDLE) {
//          throw new Exception("Cannot add channel while " + state); //$NON-NLS-1$
//      }

      // Is this an existing channel?
//      ArchiveChannel<T> channel = getChannel(channelName);

      // For the engine, channels can be in more than one group
      // if configuration matches.
//      if (channel != null)
//      {
//          final String gripe = String.format(
//                  "Group '%s': Channel '%s' already in group '%s'",
//                   group.getName(), channelName, channel.getGroup(0).getName());
//          if (channel.getEnablement() != enablement) {
//              throw new Exception(gripe + " with different enablement");
//          }
//          if (// Now monitor, but not before?
//              monitor && channel instanceof ScannedArchiveChannel
//              ||
//              // Or now scanned, but before monitor, or other scan rate?
//              !monitor
//               && (channel instanceof MonitoredArchiveChannel
//                   || ((ScannedArchiveChannel)channel).getPeriod() != period)) {
//              throw new Exception(gripe + " with different sample mechanism");
//          }
//      }
//      else
//      {   // Channel is new to this engine.
          // See if there's already a sample in the archive,
          // because we won't be able to go back-in-time before that sample.
//        IValue last_sample = null;
//
//        final IArchiveWriterService service = Activator.getDefault().getArchiveWriterService();
//
//        final ITimestamp lastTimestamp  =
//            service.getLatestTimestampForChannel(channelName);

//        if (lastTimestamp != null) {
//            // Create fake string sample with that time
//            last_sample = ValueFactory.createStringValue(last_stamp,
//                                                         ValueFactory.createOKSeverity(),
//                                                         "",
//                                                         IValue.Quality.Original,
//                                                         new String [] { "Last timestamp in archive" });
//        }

          // Determine buffer capacity
          //int buffer_capacity = (int) (write_period / period * buffer_reserve);
          // When scan or update period exceeds write period,
          // simply use the reserve for the capacity
//          if (buffer_capacity < buffer_reserve) {
//              buffer_capacity = (int)buffer_reserve;
//          }

          // Create new channel
//          if (monitor)
//          {
//              if (sample_val > 0) {
//                  channel = new DeltaArchiveChannel(channelName, enablement,
//                          buffer_capacity, last_sample, period, sample_val);
//              } else {

//
//                  channel = new MonitoredArchiveChannel<T>(channelName,
//                                                        enablement,
//                                                        buffer_capacity,
//                                                        last_sample,
//                                                        period);
////              }
//          }
//          else
//          {
//              channel = new ScannedArchiveChannel(channelName, enablement,
//                                      buffer_capacity, last_sample, period,
//                                      max_repeats);
//              scanner.add((ScannedArchiveChannel) channel, period);
//          }
//          synchronized (this)
//          {
//              channels.add(channel);
//              channel_by_name.put(channel.getName(), channel);
//          }
//          writer.addChannel(channel);
//      }
//      // Connect new or old channel to group
//      channel.addGroup(group);
//      group.add(channel);
//
//      return channel;
//  }
}
