package fiji.plugin.trackmate.tracking.oldlap.hungarian;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class MunkresKuhnTest
{
	boolean randomize = false; // makes things irreproducible...

	@Test
	public void testSimple() {
		double[][] weight = new double[][] {
			{ 1, 3, 4 },
			{ 5, 5, 5 },
			{ 6, 3, 2 }
		};

		if (randomize) {
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					weight[i][j] = Math.floor(Math.random() * 10);
				}
			}
		}

		StringBuilder builder = new StringBuilder();
		for (double[] row : weight) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(Arrays.toString(row));
		}

		MunkresKuhnAlgorithm algo = new MunkresKuhnAlgorithm();
		algo.computeAssignments(weight);

		double minWeight = algo.getTotalWeight();
		boolean found = false;
		for (int a = 0; a < 3; a++) {
			double weight1 = weight[0][a];
			for (int b = 0; b < 3; b++) {
				if (a != b) {
					int c = 3 - a - b;
					double weight2 = weight1 + weight[1][b] + weight[2][c];
					if (weight2 > minWeight)
						continue;
					assertTrue("weight: " + weight2 + ", 0-" + a + " 1-" + b + " 2-" + c + ", weights: " + builder, weight2 == minWeight);
					found = true;
				}
			}
		}
		assertTrue(found);
	}
}
