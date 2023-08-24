package org.vadere.state.psychology.perception.json;

import org.vadere.state.psychology.perception.types.Location;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.SubpopulationFilter;
import org.vadere.state.psychology.perception.types.Timeframe;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * StimulusInfo describes the stimuli that are injected into the simulation.
 * It contains a timeframe, a list of stimuli, a location and a subpopulation filter.
 * This makes it possible to inject stimuli at a specific time, at a specific location.
 * </p>
 * <p>
 * Example:
 * <code>
 * {
 *   "stimulusInfos" : [ {
 *     "timeframe" : {
 *       "startTime" : 0.0,
 *       "endTime" : 10.0,
 *       "repeat" : false,
 *       "waitTimeBetweenRepetition" : 0.0
 *     },
 *     "location" : {
 *       "areas" : [ {
 *         "x" : 0.0,
 *         "y" : 0.0,
 *         "width" : 1000.0,
 *         "height" : 500.0,
 *         "type" : "RECTANGLE"
 *       } ]
 *     },
 *     "subpopulationFilter" : {
 *       "affectedPedestrianIds" : [ ]
 *     },
 *     "stimuli" : [ {
 *       "type" : "Threat",
 *       "originAsTargetId" : -1,
 *       "loudness" : 1.0
 *     } ]
 *   } ]
 * }
 * </code>
 * </p>
 */
public class StimulusInfo {

    // Member Variables
    /**
     * <i>timeframe</i> describes the time at which the stimuli are injected.
     */
    private Timeframe timeframe;
    /**
     * <i>location</i> describes the location at which the stimuli are injected. It is a list of areas.
     */
    private Location location;
    /**
     * <i>subpopulationFilter</i> describes the subpopulation that is affected by the stimuli.
     */
    private SubpopulationFilter subpopulationFilter;
    /**
     * <i>stimuli</i> is a list of stimuli that are injected.
     */
    private List<Stimulus> stimuli;

    public StimulusInfo() {
        this(new Timeframe(), new LinkedList<>(), new Location(), new SubpopulationFilter());
    }

    public StimulusInfo(Timeframe timeframe, List<Stimulus> stimuli, Location location, SubpopulationFilter subpopulationFilter) {
        this.timeframe = timeframe;
        this.stimuli = stimuli;
        this.location = location;
        this.subpopulationFilter = subpopulationFilter;
    }

    // Getter
    public Timeframe getTimeframe() {
        return timeframe;
    }
    public List<Stimulus> getStimuli() {
        return stimuli;
    }
    public Location getLocation() {return location;}
    public SubpopulationFilter getSubpopulationFilter() {return subpopulationFilter;}



    // Setter
    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }
    public void setStimuli(List<Stimulus> stimuli) {
        this.stimuli = stimuli;
    }
    public void setLocation(Location location) {this.location = location;}
    public void setSubpopulationFilter(SubpopulationFilter subpopulationFilter) {this.subpopulationFilter = subpopulationFilter;}



}
