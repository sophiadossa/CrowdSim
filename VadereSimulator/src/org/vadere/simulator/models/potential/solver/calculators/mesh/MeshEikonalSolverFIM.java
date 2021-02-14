package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ForkJoinPool;


/**
 * This class computes the traveling time T using the fast iterative method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 */
public class MeshEikonalSolverFIM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolver<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverFIM.class);

	private int nThreds = 1;

	final String identifier;

	static {
		logger.setDebug();
	}

	/**
	 * Indicates that the computation of T has been completed.
	 */
	private boolean calculationFinished = false;

	/**
	 * The narrow-band of the fast marching method.
	 */
	private LinkedList<V> activeList;

	private int iteration = 0;
	private int nUpdates = 0;
	private final double epsilon = 0;


	// delete this, its only for logging
	private BufferedWriter bufferedWriter;
	private ArrayList<Integer> updates = new ArrayList<>();
	private ArrayList<ArrayList<Integer>> narrowBandSizes = new ArrayList<>();

	// Note: The updateOrder of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverFIM(@NotNull final String identifier,
	                            @NotNull final Collection<VShape> targetShapes,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	                            //@NotNull final Collection<VShape> destinations
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		this.activeList = new LinkedList<>();

		/*File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/floorFieldPlot/");
		try {
			bufferedWriter = IOUtils.getWriter("updates_fim.csv", dir);
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		//TODO a more clever init!
		List<V> initialVertices = new ArrayList<>();
		for(VShape shape : targetShapes) {
			getMesh().streamVertices()
					.filter(v -> shape.contains(getMesh().toPoint(v)))
					.forEach(v -> {
						for(V u : getMesh().getAdjacentVertexIt(v)) {
							initialVertices.add(u);
							setAsInitialVertex(u);
						}
						initialVertices.add(v);
						setAsInitialVertex(v);
					});
		}
		setInitialVertices(initialVertices, IDistanceFunction.createToTargets(targetShapes));
	}


	@Override
	public void solve() {
		double ms = System.currentTimeMillis();
		getTriangulation().enableCache();
		nUpdates = 0;
		//narrowBandSizes.add(new ArrayList<>());

		if(!solved || needsUpdate()) {
			if(!solved) {
				prepareMesh();
				unsolve();
				initialActiveList();
				march();
			} else if(needsUpdate()) {
				unsolve();
				initialActiveList();
				march();
			}
		}

		solved = true;
		//updates.add(nUpdates);
		double runTime = (System.currentTimeMillis() - ms);
		logger.debug("fim run time = " + runTime);
		logger.debug("#nUpdates = " + nUpdates);
		logger.debug("#nVertices = " + (getMesh().getNumberOfVertices() - (int)getMesh().streamVertices().filter(v -> isInitialVertex(v)).count()));
		/*if(iteration % 100 == 0) {
			writeNarrowBandSize();
		}
		if(iteration == 3354) {
			writeUpdates();
		}*/
		iteration++;
		//logger.debug(getMesh().toPythonTriangulation(v -> getPotential(v)));
	}

	/*private void initialActiveList() {
		for(V vertex : getInitialVertices()) {
			for(V v : getMesh().getAdjacentVertexIt(vertex)) {
				if(isUndefined(v)) {
					updatePotential(v);
				}
			}
		}
	}*/

	private void initialActiveList() {
		for(V vertex : getInitialVertices()) {
			activeList.addLast(vertex);
			//setPotential(vertex, 0);
		}
	}

	private void march() {
		ArrayList<Integer> narrowBandSize=null;
		/*if(iteration % 100 == 0) {
			narrowBandSize = narrowBandSizes.get(narrowBandSizes.size()-1);
		}*/

		while(!activeList.isEmpty()) {
			//logger.debug("#activeList = " + activeList.size());
			ListIterator<V> listIterator = activeList.listIterator();
			LinkedList<V> newActiveList = new LinkedList<>();
			while(listIterator.hasNext()) {
				V x = listIterator.next();
				double p = getPotential(x);
				double q = p;

				if(!isInitialVertex(x)) {
					q =  Math.min(p, recomputePotential(x));
					setPotential(x, q);
				}

				if (Math.abs(p - q) <= epsilon) {
					setBurned(x);
					setUnburning(x);
					// check adjacent neighbors
					for(V xn : getMesh().getAdjacentVertexIt(x)) {
						if(getPotential(xn) > getPotential(x) && !isInitialVertex(xn) && !isBurining(xn)) {
							p = getPotential(xn);
							q = recomputePotential(xn);
							if(p > q) {
								setPotential(xn, q);
								newActiveList.add(xn);
								setBurning(xn);
								if(iteration % 100 == 0) {
									narrowBandSize.add(newActiveList.size()+activeList.size());
								}

							}
						}
					}
					listIterator.remove();
					if(iteration % 100 == 0) {
						narrowBandSize.add(newActiveList.size()+activeList.size());
					}

					setBurned(x);
					setUnburning(x);
					if(!isInitialVertex(x)) {
						nUpdates++;
					}
				}
			}
			activeList.addAll(newActiveList);
		}
	}

	private void writeUpdates() {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("updates = [");
			for(int j = 0; j < updates.size(); j++) {
				builder.append(updates.get(j));
				if(j < updates.size()-1) {
					builder.append(",");
				}
			}
			builder.append("]\n");
			bufferedWriter.write(builder.toString());
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeNarrowBandSize() {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("ns = [");
			for(int j = 0; j < narrowBandSizes.get(narrowBandSizes.size()-1).size(); j++) {
				builder.append(narrowBandSizes.get(narrowBandSizes.size()-1).get(j));
				if(j < narrowBandSizes.get(narrowBandSizes.size()-1).size()-1) {
					builder.append(",");
				}
			}
			builder.append("]\n");
			bufferedWriter.write(builder.toString());
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {
			if(!isBurining(vertex)) {
				activeList.add(vertex);
			}
			setPotential(vertex, potential);
			setBurning(vertex);
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}

	@Override
	protected void compute() {
		march();
	}
}
