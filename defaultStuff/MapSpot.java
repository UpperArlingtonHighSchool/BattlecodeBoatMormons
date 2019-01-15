package bc19;

import bc19.*;

public class MapSpot {
	public int x, y, traveled, estimate;
	public MapSpot parent;

	public MapSpot(MapSpot previous, int xSpot, int ySpot, int realCost, int estimatedCost) {
		parent = previous;
		x = xSpot;
		y = ySpot;
		traveled = realCost;
		estimate = estimatedCost;
	}

	public int compareTo(MapSpot other) {
		return this.traveled + this.estimate - other.traveled - other.estimate;
	}
	
	public boolean equals(MapSpot other) {
		return this.x == other.x && this.y == other.y;
	}
}