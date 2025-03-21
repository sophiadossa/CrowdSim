package org.vadere.simulator.projects.io;

import static  org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.simulator.projects.SimulationOutput;
import org.vadere.simulator.projects.VadereProject;

/**
 *
 * @author Stefan Schuhbäck
 */
public class IOOutputTest {

	private Path projectDir;
	private Path corruptedOutput;
	private VadereProject project;

	@BeforeEach
	public void setup() throws URISyntaxException, IOException {
		projectDir = Paths.get(getClass().getResource("/data/simpleProject").toURI());
		corruptedOutput = Paths.get(getClass().getResource("/data/corruptedOutput").toURI());
		project = IOVadere.readProject(projectDir.toString());
	}

	@Test
	public void getSimulationOutput() throws Exception {
		String out1Dir = "output/test_postvis_2019-09-23_17-32-20.881";
		Optional<SimulationOutput> out1 =
				IOOutput.getSimulationOutput(project, projectDir.resolve(out1Dir).toFile());
		assertTrue(out1.isPresent());
		SimulationOutput simOut = out1.get();
		assertEquals("test_postvis",simOut.getSimulatedScenario().getName());

		String out2Dir = "output/corrupt/test_postvis_2018-01-19_13-38-01.695";
		Optional<SimulationOutput> out2 =
				IOOutput.getSimulationOutput(project, projectDir.resolve(out2Dir).toFile());
		assertFalse(out2.isPresent(),
				"The selected Directory is corrupted and should not be a valid SimulationOutput");
	}

	@Test
	public void getSimulationOutputs() throws Exception {
		ConcurrentMap<String, SimulationOutput> simOutputs;
		simOutputs = IOOutput.getSimulationOutputs(project);
		assertEquals(14,simOutputs.size(),"There should be 14 valid SimulationOutputs");

		FileUtils.copyDirectory(corruptedOutput.toFile(), projectDir.resolve("output").toFile());
		simOutputs = IOOutput.getSimulationOutputs(project);
		assertEquals(14,simOutputs.size(),"There should be 14 valid SimulationOutputs");
	}



}