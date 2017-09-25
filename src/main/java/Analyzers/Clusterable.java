package Analyzers;

public abstract class Clusterable {
	private double distanceToCentroid;
	
	public double getDistanceToCentroid() {
		return distanceToCentroid;
	}
	public void setDistanceToCentroid(double distanceToCentroid) {
		this.distanceToCentroid = distanceToCentroid;
	}
	public abstract double[] getVector();
	public abstract int getFrequency();
	public abstract void setChange(boolean isChanged);
	public abstract boolean isChanged();
}
