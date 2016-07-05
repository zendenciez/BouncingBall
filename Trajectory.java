import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;


public class Trajectory
{
	public ArrayList<Line2D.Float> trajectoryArray;
	public Point2D.Float lastPoint;
	
	public Trajectory()
	{
		trajectoryArray = new ArrayList<Line2D.Float>();
		//lastPoint = new Point2D.Float();
	}
	
	public void addPoint(Point2D.Float ptToAdd)
	{
		if(trajectoryArray != null)
		{
			if(lastPoint != null)
			{
				if(!lastPoint.equals(ptToAdd))
				{
					Line2D.Float newLine = new Line2D.Float(lastPoint, ptToAdd);
					trajectoryArray.add(newLine);
					//System.out.println(newLine.getP1() + " " + newLine.getP2());
				}
			}
			lastPoint = ptToAdd;
		}
	}
	
	public void drawLastLine(Graphics2D g2d)
	{
		
		g2d.setColor(Global.TRAJECTORY_COLOR);
		//System.out.format("Trajectory size = %d\n", trajectoryArray.size());
		for(int i = 0; i < trajectoryArray.size(); i++)
		{
			if(trajectoryArray.get(i) != null)
			{
				g2d.draw(trajectoryArray.get(i));
			}
		}

		
	}
	
	public void clearTrajectoryArray()
	{
		trajectoryArray.clear();
		lastPoint = null;
	}
	
}
