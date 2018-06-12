package org.vadere.util.triangulation.adaptive;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class Parameters {
	public final static double DPTOL = 0.001;
    public final static double TOL = .1;
    public final static double FSCALE = 1.2;
    public final static double DELTAT = 0.2;
    public final static double MIN_TRIANGLE_QUALITY = 0.1;
    public final static double MIN_FORCE_RATIO = 0.3;
    public final static double MIN_SPLIT_TRIANGLE_QUALITY = 0.4;
	public final static double h0 = 0.15;
	public final static boolean uniform = false;
	public final static String method = "Distmesh"; // "Distmesh" or "Density"
    public final static double qualityMeasurement = 0.96;
    public final static double MINIMUM = 0.25;
    public final static double DENSITYWEIGHT = 2;
    public final static int NPOINTS = 100000;
    public final static int SAMPLENUMBER = 10;
    public final static int SAMPLEDIVISION = 10;
    public final static int SEGMENTDIVISION = 0;
    public final static int MAX_NUMBER_OF_STEPS = 100;
}
