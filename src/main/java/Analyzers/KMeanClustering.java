package Analyzers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class KMeanClustering {

	public static final int COSINE_SIM = 1;
	public static final int PEARSON_COR = 2;

	private KMeanClustering() {
	}

	
	public static List<List<Clusterable>> clusterBySimilarity(int iteration,
			List<Clusterable> itemList, double threshold, int mode) {

		if (itemList == null || itemList.size() == 0)
			return null;
		int iterator = 0;
		int vectorSize = itemList.get(0).getVector().length;
		int numItems = itemList.size();
		int[] itemToCluster = new int[numItems];
		double[][] centroids = new double[itemList.size()][vectorSize];
		// initial step: Select N centroids
		Set<Integer> selected = new HashSet<>();
        Random ran = new Random(482738071923l); 
		for (int i = 0; i < itemList.size(); i++) {
			int item = 0;
			do {
				item = (int) (ran.nextDouble() * numItems);
			} while (selected.contains(item));
			selected.add(item);
			centroids[i] = itemList.get(item).getVector();
		}
		// training
		for (int i = 0; i < iteration; i++) {
			iterator++;
			// Step 1: re-assign items to clusters
			boolean terminate = true;
			for (int item = 0; item < numItems; item++) {
				double maxSimilarity = Double.MIN_VALUE;
				int maxK = 0;
				for (int k = 0; k < itemList.size(); k++) {
					double similarity = 0;
					if (mode == COSINE_SIM)
						similarity = cosineSimilarityForVectors(
								itemList.get(item).getVector(), centroids[k],
								true);
					if (mode == PEARSON_COR)
						similarity = pearsonCorrelation(itemList.get(item)
								.getVector(), centroids[k], true);
					if (similarity < threshold)
						continue;
					if (maxSimilarity < similarity) {
						maxSimilarity = similarity;
						maxK = k;
					}
				}

				if (itemToCluster[item] != maxK) {
					itemToCluster[item] = maxK;
					itemList.get(item).setDistanceToCentroid(maxSimilarity);
					terminate = false;
				}
			}

			if (terminate)
				break;
			// Step 2: recalculate centroids
			for (int k = 0; k < itemList.size(); k++) {
				Arrays.fill(centroids[k], 0);
				// int count = 0;
				for (int item = 0; item < numItems; item++) {
					if (itemToCluster[item] == k) {
						double[] itemVector = itemList.get(item).getVector();
						// count += itemList.get(item).getFrequency();
						for (int j = 0; j < vectorSize; j++) {
							centroids[k][j] += itemVector[j];
						}
					}
				}
				// for (int j = 0; j < vectorSize; j++) {
				// centroids[k][j] /= (float) count;
				// }
			}
		}
		// order by distance to centroid
		List<List<Clusterable>> clusters = new ArrayList<>();
		for (int k = 0; k < itemList.size(); k++) {
			clusters.add(new ArrayList<Clusterable>());
		}
		for (int item = 0; item < numItems; item++) {
			clusters.get(itemToCluster[item]).add(itemList.get(item));
		}

		for (int k = 0; k < itemList.size(); k++) {
			Collections.sort(clusters.get(k), new Comparator<Clusterable>() {

				@Override
				public int compare(Clusterable o1, Clusterable o2) {
					// TODO Auto-generated method stub
					return (int) Math.signum(o2.getFrequency()
							- o1.getFrequency());
				}

			});
		}
		System.out.println("--Number of Iteration = " + iterator);
		return clusters;
	}

	public static List<List<Clusterable>> clusterBySimilarity(int iteration,
			List<Clusterable> itemList, int K, int mode) {

		if (itemList == null || itemList.size() == 0)
			return null;
		int iterator = 0;
		int vectorSize = itemList.get(0).getVector().length;
		int numItems = itemList.size();
		int[] itemToCluster = new int[numItems];
		double[][] centroids = new double[K][vectorSize];
		// initial step: Select K centroids
		Set<Integer> selected = new HashSet<>();
		for (int i = 0; i < K; i++) {
			int item = 0;
			do {
				item = (int) (Math.random() * numItems);
			} while (selected.contains(item));
			selected.add(item);
			centroids[i] = itemList.get(item).getVector();
		}
		// training
		for (int i = 0; i < iteration; i++) {
			iterator++;
			// Step 1: re-assign items to clusters
			boolean terminate = true;
			for (int item = 0; item < numItems; item++) {
				double maxSimilarity = Double.MIN_VALUE;
				int maxK = 0;
				for (int k = 0; k < K; k++) {
					double similarity = 0;
					if (mode == COSINE_SIM)
						similarity = cosineSimilarityForVectors(
								itemList.get(item).getVector(), centroids[k],
								true);
					if (mode == PEARSON_COR)
						similarity = pearsonCorrelation(itemList.get(item)
								.getVector(), centroids[k], true);
					if (maxSimilarity < similarity) {
						maxSimilarity = similarity;
						maxK = k;
					}
				}

				if (itemToCluster[item] != maxK) {
					itemToCluster[item] = maxK;
					itemList.get(item).setDistanceToCentroid(maxSimilarity);
					terminate = false;
				}
			}

			if (terminate)
				break;
			// Step 2: recalculate centroids
			for (int k = 0; k < K; k++) {
				Arrays.fill(centroids[k], 0);
				// int count = 0;
				for (int item = 0; item < numItems; item++) {
					if (itemToCluster[item] == k) {
						double[] itemVector = itemList.get(item).getVector();
						// count += itemList.get(item).getFrequency();
						for (int j = 0; j < vectorSize; j++) {
							centroids[k][j] += itemVector[j];
						}
					}
				}
				// for (int j = 0; j < vectorSize; j++) {
				// centroids[k][j] /= (float) count;
				// }
			}
		}
		// order by distance to centroid
		List<List<Clusterable>> clusters = new ArrayList<>();
		for (int k = 0; k < K; k++) {
			clusters.add(new ArrayList<Clusterable>());
		}
		for (int item = 0; item < numItems; item++) {
			clusters.get(itemToCluster[item]).add(itemList.get(item));
		}

		for (int k = 0; k < K; k++) {
			Collections.sort(clusters.get(k), new Comparator<Clusterable>() {

				@Override
				public int compare(Clusterable o1, Clusterable o2) {
					// TODO Auto-generated method stub
					return (int) Math.signum(o2.getFrequency()
							- o1.getFrequency());
				}

			});
		}
		System.out.println("--Number of Iteration = " + iterator);
		return clusters;
	}

	/**
	 * Computes the cross correlation between sequences a and b.
	 * https://www.ee.columbia
	 * .edu/~ronw/code/dev/MEAPsoft/src/com/meapsoft/DSP.java
	 */
	public static float[] xcorr(float[] a, float[] b) {
		int len = a.length;
		if (b.length > a.length)
			len = b.length;

		return xcorr(a, b, len - 1);

		// // reverse b in time
		// double[] brev = new double[b.length];
		// for(int x = 0; x < b.length; x++)
		// brev[x] = b[b.length-x-1];
		//
		// return conv(a, brev);
	}

	/**
	 * Computes the cross correlation between sequences a and b. maxlag is the
	 * maximum lag to
	 */
	public static float[] xcorr(float[] a, float[] b, int maxlag) {
		float[] y = new float[2 * maxlag + 1];
		Arrays.fill(y, 0);

		for (int lag = b.length - 1, idx = maxlag - b.length + 1; lag > -a.length; lag--, idx++) {
			if (idx < 0)
				continue;

			if (idx >= y.length)
				break;

			// where do the two signals overlap?
			int start = 0;
			// we can't start past the left end of b
			if (lag < 0) {
				// System.out.println("b");
				start = -lag;
			}

			int end = a.length - 1;
			// we can't go past the right end of b
			if (end > b.length - lag - 1) {
				end = b.length - lag - 1;
				// System.out.println("a "+end);
			}

			// System.out.println("lag = " + lag +": "+ start+" to " +
			// end+"   idx = "+idx);
			for (int n = start; n <= end; n++) {
				// System.out.println("  bi = " + (lag+n) + ", ai = " + n);
				y[idx] += a[n] * b[lag + n];
			}
			// System.out.println(y[idx]);
		}

		return (y);
	}

	// a.length == b.length
	public static double pearsonCorrelation(double[] x, double[] y,
			boolean normalize) {
		double xmean = 0;
		double ymean = 0;
		for (int i = 0; i < x.length; i++) {
			xmean += x[i];
			ymean += y[i];
		}
		xmean /= x.length;
		ymean /= x.length;

		double numerator = 0;
		double xsquare = 0, ysquare = 0;
		double[] xError = new double[x.length];
		double[] yError = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			xError[i] = x[i] - xmean;
			yError[i] = y[i] - ymean;
		}
		for (int i = 0; i < x.length; i++) {
			numerator += xError[i] * yError[i];
			xsquare += xError[i] * xError[i];
			;
			ysquare += yError[i] * yError[i];
		}
		double denominator = Math.sqrt(xsquare) * Math.sqrt(ysquare);
		if (denominator == Double.NaN)
			return 0;
		if (!normalize)
			return numerator / denominator;
		else
			return (1 + numerator / denominator) / 2;

	}

	public static double cosineSimilarityForVectors(double[] vector1,
			double[] vector2, boolean normalize) {

		double sim = 0, square1 = 0, square2 = 0;

		if (vector1 == null || vector2 == null)
			return 0;
		for (int i = 0; i < vector1.length; i++) {
			square1 += vector1[i] * vector1[i];
			square2 += vector2[i] * vector2[i];
			sim += vector1[i] * vector2[i];
		}
		if (sim == 0)
			return 0;
		if (!normalize)
			return sim / Math.sqrt(square1) / Math.sqrt(square2);
		else
			return (1 + sim / Math.sqrt(square1) / Math.sqrt(square2)) / 2;
	}
	/*
	 * return -1 if either vector1 or vector2 is null; 
	 */
	public static double euclideanDistance(float[] vector1, float[] vector2){
		double dist = 0;
		if (vector1 == null || vector2 == null)
			return -1;
		float[] squareVector = new float[vector1.length];
		for(int i = 0; i <squareVector.length; i++){
			squareVector[i] = (vector1[i]-vector2[i]) * (vector1[i]-vector2[i]);
		}
		for(int i = 0; i <squareVector.length; i++){
			dist+= squareVector[i];
		}
		return Math.sqrt(dist);
	}
}
