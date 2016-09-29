package org.vadere.simulator.models.seating;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.ActiveCallback;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.Seat;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetListener;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TrainGeometry;
import org.vadere.util.math.TruncatedNormalDistribution;
import org.vadere.util.reflection.DynamicClassInstantiator;

/**
 * This model can only be used with train scenarios complying with scenarios generated by Traingen.
 * 
 * To enable this model, add this model's class name to the main model's submodel list and
 * load a train topography.
 *
 */
public class SeatingModel implements ActiveCallback, Model {

	private static final int[] SEAT_INDEXES = {0, 1, 2, 3};

	private final Logger log = Logger.getLogger(SeatingModel.class);
	
	private AttributesSeating attributes;
	private TrainModel trainModel;
	private Random random;
	/** Used for distributions from Apache Commons Math. */
	private RandomGenerator rng;

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		this.attributes = Model.findAttributes(attributesList, AttributesSeating.class);
		
		DynamicClassInstantiator<TrainGeometry> instantiator = new DynamicClassInstantiator<>();
		TrainGeometry trainGeometry = instantiator.createObject(attributes.getTrainGeometry());
		try {
			trainModel = new TrainModel(topography, trainGeometry);
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Topography is corrupt or not a %s train.",
					trainGeometry.getClass().getSimpleName()), e);
		}

		this.random = random;
		this.rng = new JDKRandomGenerator(random.nextInt());
		
		for (final Target target : trainModel.getCompartmentTargets()) {
			target.addListener(compartmentTargetListener);
		}

		for (final Seat seat : trainModel.getSeats()) {
			seat.getAssociatedTarget().addListener(seatTargetListener);
		}

	}

	@Override
	public void preLoop(double simTimeInSec) {
		// before simulation
	}

	@Override
	public void postLoop(double simTimeInSec) {
		// after simulation
	}

	@Override
	public void update(double simTimeInSec) {
		// choose compartment for those peds without a target
		trainModel.getPedestrians().stream()
				.filter(this::hasNoTargetAssigned)
				.forEach(this::assignCompartmentTarget);
		
		// the next steps are done by target listeners registered in initialize()
	}
	
	private void assignCompartmentTarget(Pedestrian p) {
		final int entranceAreaIndex = trainModel.getEntranceAreaIndexForPerson(p);
		final Compartment compartment = chooseCompartment(p, entranceAreaIndex);
		logAssigningCompartment(p, compartment);
		p.addTarget(compartment.getCompartmentTarget());
	}

	private void logAssigningCompartment(Pedestrian p, final Compartment compartment) {
		logDebug("Assigning compartment %d to pedestrian %d", compartment.getIndex(), p.getId());
	}

	private void assignSeatTarget(Pedestrian p) {
		final Compartment compartment = trainModel.getCompartment(p);
		if (compartment.getPersonCount() == Compartment.MAX_PERSONS_PER_COMPARTMENT) {
			logDebug("Compartment %d is full. No seat available for pedestrian %d.",
					compartment.getIndex(), p.getId());
			proceedToNextCompartmentIfPossible(p);
			return;
		}
		
		final SeatGroup seatGroup = chooseSeatGroup(compartment);
		final Seat seat = chooseSeat(seatGroup);
		logDebug("Assigning seat %d.%d to pedestrian %d", compartment.getIndex(),
				seat.getSeatNumberWithinCompartment(), p.getId());
		p.addTarget(seat.getAssociatedTarget());
	}
	
	private void proceedToNextCompartmentIfPossible(Pedestrian p) {
		final int fromEntranceAreaIndex = trainModel.getEntranceAreaIndexForPerson(p);
		final int compartmentIndex = trainModel.getCompartment(p).getIndex();
		if (compartmentIndex > 0 && compartmentIndex < trainModel.getCompartmentCount() - 1) {
			final int direction = getDirectionFromEntranceAreaToCompartment(fromEntranceAreaIndex, compartmentIndex);
			final Compartment nextCompartment = trainModel.getCompartment(compartmentIndex + direction);
			logAssigningCompartment(p, nextCompartment);
			p.addTarget(nextCompartment.getCompartmentTarget());
		}
	}

	private int getDirectionFromEntranceAreaToCompartment(int entranceAreaIndex, int compartmentIndex) {
		// entrance areas:    0   1   2   3
		// compartments:    0   1   2   3   4
		if (compartmentIndex <= entranceAreaIndex)
			return -1;
		else
			return +1;
	}

	private boolean hasNoTargetAssigned(Pedestrian p) {
		return p.getTargets().isEmpty();
	}
	
	public TrainModel getTrainModel() {
		return trainModel;
	}

	public Compartment chooseCompartment(Pedestrian person, int entranceAreaIndex) {
		// entrance areas:    0   1   2   3
		// compartments:    0   1   2   3   4
		// left- and rightmost compartments are "half-compartments"
		
		final int entranceAreaCount = trainModel.getEntranceAreaCount();

		final double distributionMean = entranceAreaIndex + 0.5;
		final double distributionSd = entranceAreaCount / 2.0;
		final RealDistribution distribution = new TruncatedNormalDistribution(rng, distributionMean, distributionSd,
				0, entranceAreaCount, 100);

		final double value = distribution.sample();
		final int compartmentIndex = (int) Math.round(value);
		return trainModel.getCompartment(compartmentIndex);
	}
	
	public SeatGroup chooseSeatGroup(Compartment compartment) {
		final List<Pair<Boolean, Double>> valuesAndProbabilities = attributes.getSeatGroupChoice();
		final EnumeratedDistribution<Boolean> distribution = new EnumeratedDistribution<>(rng, valuesAndProbabilities);
		
		List<SeatGroup> seatGroups = compartment.getSeatGroups().stream()
				.filter(sg -> sg.getPersonCount() < 4)
				.collect(Collectors.toList());

		if (seatGroups.isEmpty()) {
			throw new IllegalStateException("No seats available in given compartment.");
		}
		
		while (seatGroups.size() > 1) {
			final int minPersonCount = getSeatGroupMinPersonCount(seatGroups);

			if (allSeatGroupPersonCountsEquals(seatGroups)) {
				return drawRandomElement(seatGroups);
			}

			if (distribution.sample()) {
				// choice for seat group with minimal number of other passengers
				final List<SeatGroup> minSeatGroups = seatGroups.stream()
						.filter(sg -> sg.getPersonCount() == minPersonCount)
						.collect(Collectors.toList());
				return drawRandomElement(minSeatGroups);
			} else {
				seatGroups = seatGroups.stream()
						.filter(sg -> sg.getPersonCount() != minPersonCount)
						.collect(Collectors.toList());
			}
		}
		
		return seatGroups.get(0);
	}

	private boolean allSeatGroupPersonCountsEquals(List<SeatGroup> result) {
		return result.stream().mapToInt(SeatGroup::getPersonCount).distinct().count() == 1;
	}

	private int getSeatGroupMinPersonCount(List<SeatGroup> result) {
		return result.stream()
				.mapToInt(SeatGroup::getPersonCount)
				.min().getAsInt();
	}
	
	private <T> T drawRandomElement(List<T> list) {
		return list.get(random.nextInt(list.size()));
	}

	public Seat chooseSeat(SeatGroup seatGroup) {
		final int personsSitting = seatGroup.getPersonCount();
		switch (personsSitting) {
		case 0:
			return chooseSeat0(seatGroup);

		case 1:
			return chooseSeat1(seatGroup);

		case 2:
			return chooseSeat2(seatGroup);

		case 3:
			return chooseSeat3(seatGroup);

		default:
			assert personsSitting == 4;
			throw new IllegalStateException("Seat group is already full. This method should not have been called!");
		}
	}

	private Seat chooseSeat0(SeatGroup seatGroup) {
		final double[] probabilities = attributes.getSeatChoice0();
		final EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(rng, SEAT_INDEXES, probabilities);
		return seatGroup.getSeat(distribution.sample());
	}

	private Seat chooseSeat1(final SeatGroup seatGroup) {
		final EnumeratedDistribution<SeatRelativePosition> distribution = new EnumeratedDistribution<>(rng, attributes.getSeatChoice1());
		final SeatRelativePosition relativePosition = distribution.sample();
		return seatGroup.seatRelativeTo(seatGroup.getTheOccupiedSeat(), relativePosition);
	}

	private Seat chooseSeat2(final SeatGroup seatGroup) {
		if (seatGroup.onlySideChoice()) {
			// choice only between window/aisle
			final EnumeratedDistribution<SeatSide> distribution = new EnumeratedDistribution<>(rng, attributes.getSeatChoice2Side());
			SeatSide side = distribution.sample();
			return seatGroup.availableSeatAtSide(side);

		} else if (seatGroup.onlyFacingDirectionChoice()) {
			// choice only between forward/backward
			final EnumeratedDistribution<SeatFacingDirection> distribution = new EnumeratedDistribution<>(rng, attributes.getSeatChoice2FacingDirection());
			SeatFacingDirection facingDirection = distribution.sample();
			return seatGroup.availableSeatAtFacingDirection(facingDirection);

		} else {
			// choice between both window/aisle and forward/backward
			return seatGroup.getTheTwoAvailableSeats().get(random.nextInt(2));
		}
	}

	private Seat chooseSeat3(final SeatGroup seatGroup) {
		return seatGroup.getTheAvailableSeat();
	}

	private void sitDownIfPossible(Pedestrian pedestrian, Seat seat) {
		if (seat.getSittingPerson() == null) {
			seat.setSittingPerson(pedestrian);
		} else {
			// try other empty seat in same seat group
			SeatGroup seatGroup = seat.getSeatGroup();
			final Compartment compartment = trainModel.getCompartment(pedestrian.getTargets().get(pedestrian.getTargets().size()));
//			compartment.get
			// if there is none, go back to compartment target to trigger assign seat
			throw new RuntimeException("not yet implemented");
		}
	}

	private void logDebug(String formatString, Object... args) {
		log.debug(String.format(formatString, args));
	}
	
	private final TargetListener compartmentTargetListener = new TargetListener() {
		@Override
		public void reachedTarget(Target target, Agent agent) {
			assignSeatTarget((Pedestrian) agent);
		}
	};

	private final TargetListener seatTargetListener = new TargetListener() {
		@Override
		public void reachedTarget(Target target, Agent agent) {
			sitDownIfPossible((Pedestrian) agent, trainModel.getSeatForTarget(target));
		}
	};


}
