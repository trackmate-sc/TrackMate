package fiji.plugin.trackmate.tracking.hungarian;

import java.util.Arrays;

public class MunkresKuhnTest
{
	public static void main(String[] args) {
		double[][] weight = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++)
				weight[i][j] = Math.floor(Math.random() * 10);
			System.err.println(Arrays.toString(weight[i]));
		}

		MunkresKuhnAlgorithm algo = new MunkresKuhnAlgorithm();
		algo.computeAssignments(weight);
		for (int x = 0; x < algo.M; x++)
			System.err.println("map " + x + " to " + algo.matchingY[x]);
		double minWeight = algo.getTotalWeight();
		System.err.println("weight: " + minWeight);
		for (int a = 0; a < 3; a++) {
			double weight1 = weight[0][a];
			for (int b = 0; b < 3; b++)
				if (a != b) {
					int c = 3 - a - b;
					double weight2 = weight1 + weight[1][b] + weight[2][c];
					if (weight2 > minWeight)
						continue;
					if (weight2 < minWeight)
						System.err.println("ERROR: " + weight2);
					System.err.println("0-" + a + " 1-" + b + " 2-" + c);
				}
		}
	}
}
