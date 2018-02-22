package fiji.plugin.trackmate.tracking.oldlap.hungarian;

/** This implements optimal matching between two sets given a weight matrix
 * (where the goal is to minimize the cumulative weight of the matches).
 * <p>
 * This implements the improved O(n^3) algorithm by Kuhn and Munkres, as
 * described here:
 *
 * http://www.cse.ust.hk/~golin/COMP572/Notes/Matching.pdf
 * <p>
 * A few definitions: a labeling (AKA potential) of the vertices is a
 * real-valued function l such that weight(x, lY) &lt;= l(x) + l(lY). Those edges
 * whose weight is equal to the sum of the connected vertices' labelings are
 * called <u>tight</u>.
 * <p>
 * The equality graph of a labeling is the graph consisting of the tight edges
 * and the vertices they connect.
 * <p>
 * An alternating path is a path along edges that alternate between X and Y.
 * In the context of the Hungarian algorithm, all alternating paths begin and
 * end in X.
 * <p>
 * An alternating tree is a set of alternating paths all beginning in the same
 * root in X. In the context of this algorithm, all alternating trees visit
 * each node at most once, i.e. there is at most one incoming and one outgoing
 * edge for each vertex.
 * <p>
 * The alternating trees in the Hungarian algorithm have the property that all
 * edges from X to Y are <u>matches</u>, so that the current matching can be
 * extracted from the alternating tree by taking every other edge.
 *
 * The basic idea is to evolve a labeling together with the matching. In each
 * iteration, either more vertices are matched, or the labeling is changed such
 * that its egality graph contains more edges useful for the matching (an edge
 * is not very useful if it connects two vertices which are already spanned by
 * the current alternating tree).
 * <p>
 * The details of this idea are described eloquently by András Frank in
 *
 *	http://www.cs.elte.hu/egres/tr/egres-04-14.pdf
 * <p>
 * Note that the term <i>exposed</i> simply means "unmatched", and the term
 * <i>weighted-covering</i> refers to the labeling, while <i>orienting
 * edges</i> denotes the building of the alternating tree:
 * <p>
 * "In a general step, Kuhn’s algorithm also has a weighted-covering π and
 * considers the subgraph Gπ of tight edges (on node set S ∪ T). Let M be a
 * matching in Gπ. Orient the elements of M from T to S while all other edges
 * of Gπ from S to T. Let RS ⊆ S and RT ⊆ T denote the set of nodes exposed by
 * M in S and in T, respectively. Let Z denote the set of nodes reachable in
 * the resulting digraph from RS by a directed path (that can be computed by a
 * breadth-first search, for example).
 * <p>
 *  If RT ∩ Z is non-empty, then we have obtained a path P consisting of tight
 *  edges that alternates in M. The symmetric difference of P and M is a
 *  matching M of Gπ consisting of one more edge than M does. The procedure is
 *  then iterated with this M. If RT ∩ Z is empty, then revise π as follows.
 *  Let ∆ := min{π(u) + π(v) − c(uv): u ∈ Z ∩ S, v ∈ T − Z}. Decrease
 *  (increase, respectively) the π-value of the elements of S ∩ Z (of T ∩ Z,
 *  resp.) by ∆. The resulting π is also a weighted-covering. Construct the
 *  subgraph of Gπ and iterate the procedure with π and with the unchanged M."
 * <p>
 * The first clever idea, therefore, is to find an alternating path in the
 * egality graph whose first (and likewise, whose last) edge is not a matching
 * but every other edge is. By inverting the meaning of those edges (matches
 * become non-matches, and vice versa), there will be one more match in the
 * end.
 * <p>
 * The second clever idea is that if no such alternating path can be found (in
 * the complete alternating tree using the current matching, starting from an
 * unmatched x as root), then the labeling can be adjusted easily to retain the
 * part of the egality graph which is covered by the tree, but adding one edge
 * to the egality graph such that the first idea catches.
 * <p>
 * Note that in the implementation, we follow the naming of the aforementioned
 * Matching.pdf, so our <i>S</i> is Frank's <i>S ∩ Z</i>, etc.
 * <p>
 * Also note that the secret to the O(n^3) instead of the original O(n^4) lies
 * in the use of the <i>slack</i> array, which is really just a cache for the
 * values of ∆.
 * <p>
 * Copyright 2011 (C) Johannes Schindelin
 * License: GPLv3
 * @author Johannes Schindelin
 */

public class MunkresKuhnAlgorithm implements AssignmentAlgorithm {
	public final double NO_EDGE_VALUE = Double.MAX_VALUE;

	protected int M, N; // M is the size of X, and N the size of Y
	protected double[][] weight;
	protected int[] matchingY, matchingX;

	protected double[] labelingX, labelingY;
	protected double[] slack; // called \slack_l in the paper
	protected int[] slackX;
	protected boolean[] S, T;
	protected int[] previousX; // for the alternating path

	protected int[] queue;
	protected int x, queueStart, queueEnd;

	@Override
	public int[][] computeAssignments(double[][] costMatrix) {
		weight = costMatrix;
		M = weight.length;
		if (M == 0) {
			// no spot
			return new int[][] { {  } };
		}
		N = weight[0].length;
		
		if (M <= 1 && N <= 1) {
			// no spot
			return new int[][] { {  } };
		}
		
		initialize();
		calculate();

		int[][] result = new int[matchingY.length][];
		int counter = 0;
		for (int lX = 0; lX < matchingY.length; lX++)
			if (matchingY[lX] >= 0 && weight[lX][matchingY[lX]] != NO_EDGE_VALUE)
				result[counter++] = new int[] { lX, matchingY[lX] };
		if (counter < result.length) {
			int[][] newResult = new int[counter][];
			System.arraycopy(result, 0, newResult, 0, counter);
			result = newResult;
		}

		return result;
	}

	public double getTotalWeight() {
		double result = 0;
		for (int lX = 0; lX < M; lX++)
			result += -labelingX[lX];
		for (int lY = 0; lY < M; lY++)
			result += -labelingY[lY];
		return result;
	}

	final protected void initialize() {
		matchingY = new int[M];
		matchingX = new int[N];
		for (int i = 0; i < matchingX.length; i++)
			matchingX[i] = -1;
		for (int i = 0; i < matchingY.length; i++)
			matchingY[i] = -1;

		labelingX = new double[M];
		labelingY = new double[N];

		for (int lX = 0; lX < M; lX++) {
			labelingX[lX] = -weight[lX][0];
			for (int lY = 1; lY < N; lY++)
				if (labelingX[lX] < -weight[lX][lY])
					labelingX[lX] = -weight[lX][lY];
		}

		slack = new double[N];
		slackX = new int[N];

		S = new boolean[M];
		T = new boolean[N];

		previousX = new int[N];

		queue = new int[M + N];
	}

	final protected void calculate() {
		for (int matches = 0; matches < M && matches < N; matches++) {
			// pick free vertex
			int lX = findUnmatchedX();

			// initialize S and T
			for (int i = 0; i < S.length; i++)
				S[i] = false;
			for (int i = 0; i < T.length; i++)
				T[i] = false;
			S[lX] = true;

			for (int lY = 0; lY < N; lY++) {
				slack[lY] = labelingX[lX] + labelingY[lY] - -weight[lX][lY];
				slackX[lY] = lX;
			}

			startBreadthFirstSearch(lX);

			for (;;) {
				int lY = findY();
				if (lY >= 0)
					previousX[lY] = queue[queueStart];
				else {
					lY = updateLabels();
					if (lY < 0) // no unmatched lY was found, continue breadth-first search
						continue;
				}
				if (matchingX[lY] < 0) {
					augmentPath(lY);
					break;
				}
				extendAlternatingTree(lY, matchingX[lY]);
			}
		}
	}

	final protected int findUnmatchedX() {
		for (int lX = 0; lX < M; lX++)
			if (matchingY[lX] < 0)
				return lX;
		return -1;
	}

	// start breadth-first search
	final protected void startBreadthFirstSearch(final int lX) {
		queueStart = queueEnd = 0;
		queue[queueEnd++] = lX;
	}

	// find a lY that is not in the alternating tree yet
	final protected int findY() {
		while (queueStart < queueEnd) {
			int lX = queue[queueStart];
			for (int lY = 0; lY < N; lY++)
				if (!T[lY] && isTight(lX, lY))
					return lY;
			queueStart++;
		}
		queueStart = queueEnd = 0;
		return -1;
	}

	final protected boolean isTight(final int lX, final int lY) {
		return -weight[lX][lY] == labelingX[lX] + labelingY[lY];
	}

	final protected int updateLabels() {
		double delta = Double.MAX_VALUE;

		for (int lY = 0; lY < N; lY++)
			if (!T[lY] && delta > slack[lY])
				delta = slack[lY];
		for (int lX = 0; lX < M; lX++)
			if (S[lX])
				labelingX[lX] -= delta;
		for (int lY = 0; lY < N; lY++)
			if (T[lY])
				labelingY[lY] += delta;
			else
				slack[lY] -= delta; // slackX does not change!
		// need another loop to keep the slack array intact (extending the tree changes it)
		for (int lY = 0; lY < N; lY++)
			if (!T[lY] && slack[lY] == 0) {
				previousX[lY] = slackX[lY];
				// if lY is unmatched, we can return straight away, since the path
				// will be augmented and the current tree will be abandoned anyway
				if (matchingX[lY] < 0)
					return lY;
				extendAlternatingTree(lY, matchingX[lY]);
			}
		return -1;
	}

	final protected void augmentPath(int lY) {
		while (lY >= 0) {
			int lX = previousX[lY];
			int nextY = matchingY[lX];
			matchingX[lY] = lX;
			matchingY[lX] = lY;
			lY = nextY;
		}
	}

	final protected void extendAlternatingTree(final int lY, final int z) {
		T[lY] = true;
		S[z] = true;
		queue[queueEnd++] = z;
		for (int y2 = 0; y2 < N; y2++)
			if (!T[y2] && slack[y2] > labelingX[z] + labelingY[y2] - -weight[z][y2]) {
				slack[y2] = labelingX[z] + labelingY[y2] - -weight[z][y2];
				slackX[y2] = z;
			}
	}

	protected boolean verifySlack() {
		boolean result = true;
		for (int lY = 0; lY < N; lY++)
			if (!T[lY]) {
				double min = Double.MAX_VALUE;
				int minX = -1;
				for (int lX = 0; lX < M; lX++)
					if (S[lX] && min > labelingX[lX] + labelingY[lY] - -weight[lX][lY]) {
						min = labelingX[lX] + labelingY[lY] - -weight[lX][lY];
						minX = lX;
					}
				if (minX < 0)
					continue;
				if (Math.abs(slack[lY] - min) / (Math.abs(slack[lY]) + 1e-7) > 1e-5) {
					System.err.println("ERROR: slack[" + lY + "] should be " + min + " but is " + slack[lY]);
					result = false;
				}
				if (slackX[lY] != minX && (labelingX[slackX[lY]] + labelingY[lY] - -weight[slackX[lY]][lY]
						!= labelingX[minX] + labelingY[lY] - -weight[minX][lY])) {
					System.err.println("ERROR: slackX[" + lY + "] should be " + minX + " but is " + slackX[lY]);
					result = false;
				}
			}
		return result;
	}

	protected boolean verifyMatching() {
		boolean result = true;
		for (int lX = 0; lX < M; lX++)
			if (matchingY[lX] >= 0 && matchingX[matchingY[lX]] != lX) {
				System.err.println("error: x = " + lX + " matches " + matchingY[lX] + ", which matches " + matchingX[matchingY[lX]]);
				result = false;
			}
		for (int lY = 0; lY < N; lY++)
			if (matchingX[lY] >= 0 && matchingY[matchingX[lY]] != lY) {
				System.err.println("error: lY = " + lY + " matches " + matchingX[x] + ", which matches " + matchingY[matchingX[lY]]);
				result = false;
			}
		return result;
	}

	protected String equalityGraph() {
		String message = "[";
		for (int lX = 0; lX < M; lX++)
			for (int lY = 0; lY < N; lY++)
				if (labelingX[lX] + labelingY[lY] == -weight[lX][lY])
					message += " " + lX + "-" + lY;
		message += " ]";
		return message;
	}

	protected String alternatingPath(int lY) {
		String result = " ]";
		while (lY >= 0) {
			int lX = previousX[lY];
			int nextY = matchingY[lX];
			result = " " + lX + "-" + lY + result;
			lY = nextY;
		}
		return "[" + result;
	}
}
