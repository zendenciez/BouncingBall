import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.Point2D.Float;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import java.lang.Math;

class Global
{
    public static int TITLE_BAR_WIDTH = 15;
    public static int SCREEN_WIDTH = 900;
    public static int SCREEN_HEIGHT = 700;
    public static int DX = 0;
    public static int DY = 3;
    public static int GRAVITY = 3;
    public static int TIME_TICK = 20;
    public static float DIAMETER = 30;
    public static float ELASTICITY_C = .9f;
    public static float FRICTION = .01f;
    public static float MOUSE_DAMPENER = .1f;
    public static float DENSITY = .2f;
    public static float BALL_WEIGHT = DIAMETER*DENSITY;
    public static float D_OMEGA = (float)((-1/(DIAMETER/2))*(180/Math.PI)); //ratio of angular velocity over velocity
    public static float TRACTOR_BEAM_CONSTANT_VELO = 15;
    public static Color B_COLOR = Color.MAGENTA;
    public static Color BGROUND_COLOR = Color.black;
    public static Color L_COLOR = Color.cyan;
    public static Color TRAJECTORY_COLOR = Color.green;
}

class RotatingLine extends Line2D.Float
{
    public AffineTransform aT;
    public Point2D.Float ctr;
    public float lineLength = 0f;
    public float lineAngle = 0f;
    
    public float length()
    {
        return (float)Math.sqrt(Math.pow((this.y2-this.y1), 2) + Math.pow((this.x2 - this.x1), 2));
    }
    
    
    private void updateLineCtr()
    {
        ctr.x = (this.x1 + this.x2)/2;
        ctr.y = (this.y1 + this.y2)/2;
    }
    
    private void initLine()
    {
        lineLength = this.length();
        aT = new AffineTransform();
        ctr = new Point2D.Float();
        updateLineCtr();
    }
    
    public void moveLineX(float dx)
    {
        this.x1 += dx;
        this.x2 += dx;
    }
    
    public void moveLineY(float dy)
    {
        this.y1 += dy;
        this.y2 += dy;
    }
    
    public RotatingLine(float x1, float y1, float x2, float y2)
    {
        super(x1, y1, x2, y2);
        initLine();
    }
    
    public void rotateLine(float omega)
    {
        updateLineCtr();
        lineAngle = (lineAngle + omega)%360;
        this.x1 = (float)(ctr.x + (lineLength/2)*Math.cos(Math.toRadians(lineAngle)));
        this.x2 = (float)(ctr.x - (lineLength/2)*Math.cos(Math.toRadians(lineAngle)));
        this.y1 = (float)(ctr.y - (lineLength/2)*Math.sin(Math.toRadians(lineAngle)));
        this.y2 = (float)(ctr.y + (lineLength/2)*Math.sin(Math.toRadians(lineAngle)));
    }
    
}

class BallLine extends Ball
{
    public RotatingLine rLine;
    public Point2D.Float ctr;
    
    public Point2D.Float getBallCtr()
    {
    	Point2D.Float ctr = new Point2D.Float();
    	ctr.x = this.x + this.width/2;
    	ctr.y = this.y + this.height/2;
    	return ctr;
    }
    
    public boolean contains(int x, int y)
    {
        boolean y_check = (y >= this.y) && (y <= this.y + this.height);
        boolean x_check = (x >= this.x) && (x <= this.x + this.width);
        return(y_check && x_check);
    }
    
    public BallLine(float x, float y, float w, float h)
    {
        super(x, y, w, h);
        rLine = new RotatingLine(x, y + h/2, x + w, y+ h/2);
    }
    
    public void setPosX(float x)
    {
        rLine.moveLineX(x - this.x);
        super.x = x;
    }
    
    public void setPosY(float y)
    {
        rLine.moveLineY(y - this.y);
        super.y = y;
    }
    
    public void moveX(float dx)
    {
        float omega = dx*Global.D_OMEGA;
        rLine.moveLineX(dx);
        rLine.rotateLine(omega);
        super.x += dx;
    }
    
    public void moveY(float dy)
    {
        rLine.moveLineY(dy);
        //float omega = dy*Global.D_OMEGA;
        //rLine.rotateLine(omega);
        super.y += dy;
    }
}

class Ball extends Ellipse2D.Float implements Shape
{

    public boolean contains(int x, int y)
    {
        boolean y_check = (y >= this.y) && (y <= this.y + this.height);
        boolean x_check = (x >= this.x) && (x <= this.x + this.width);
        return(y_check && x_check);
    }
    
    public Ball(float x, float y, float w, float h)
    {
        super(x, y, w, h);
    }
    
    public void setPosX(float x)
    {
        super.x = x;
    }
    
    public void setPosY(float y)
    {
        super.y = y;
    }
    
    public void moveX(float dx)
    {
        super.x += dx;
    }
    
    public void moveY(float dy)
    {
        super.y += dy;
    }
}

class BallAnimator extends JPanel
{
    public BallLine my_ball;
    public BallRunnable b_run;
    public MouseVelocity mv;
    public BallMouseDetector md;
    public RenderingHints rh;
    public RotatingLine rLine;
    public Trajectory t;
    
    class BallRunnable implements Runnable
    {
        Thread ball_run;
        private float dx; //updated x velocity
        private float dy; //updated y velocity
        private float dx0; //initial x velocity
        private float dy0; //initial y velocity
        private Bounds wall_y_bound;
        private Bounds wall_x_bound;
        private MouseVelocity runnable_mv;
        private BallLine runnable_b;
        private Trajectory runnable_t;
        
        private class Bounds
        {
            public Bounds(int l, int h)
            {
                lower_bounds = l;
                upper_bounds = h;
            }
            
            public int lower_bounds;
            public int upper_bounds;
        }
        
        private void checkBounds()
        {
            if(runnable_b.x <= wall_x_bound.lower_bounds)
            {
                dx = -1*dx*Global.ELASTICITY_C;
                runnable_b.setPosX(wall_x_bound.lower_bounds);
            }
            if(runnable_b.x + runnable_b.width >= wall_x_bound.upper_bounds)
            {
                dx = -1*dx*Global.ELASTICITY_C;
                runnable_b.setPosX(wall_x_bound.upper_bounds - runnable_b.width);
            }
            if(runnable_b.y <= wall_y_bound.lower_bounds)
            {
                dy = -1*dy*Global.ELASTICITY_C;
                runnable_b.setPosY(wall_y_bound.lower_bounds);
            }
            if(runnable_b.y + runnable_b.height >= wall_y_bound.upper_bounds)
            {
                dy = -1*dy*Global.ELASTICITY_C;
                runnable_b.setPosY(wall_y_bound.upper_bounds - runnable_b.height);
                applyFriction();
            }
        }
        
        private void calculateTractorBeamTrajectory()
        {
        	float delta_x = runnable_mv.mouse_tractor_beam_displacement.x - runnable_b.x;
        	float delta_y = runnable_mv.mouse_tractor_beam_displacement.y - runnable_b.y;
        	float vector_magnitude = (float)Math.sqrt(Math.pow(delta_x, 2) + Math.pow(delta_y,2));
        	float magnitude_scale = (float)(Global.TRACTOR_BEAM_CONSTANT_VELO/vector_magnitude);
        	
        	if(magnitude_scale > 1)
        	{
        		runnable_mv.start_tractor_beam = false;
        		//runnable_b.x = runnable_mv.mouse_tractor_beam_displacement.x;
        		//runnable_b.y = runnable_mv.mouse_tractor_beam_displacement.y;
        		dx = 0;
        		dy = 0;
        	}
        	else
        	{
        		dx = delta_x*magnitude_scale;
        		dy = delta_y*magnitude_scale;
        	}
        }
        
        private void calculateVelocity()
        {
            dx = (float)(runnable_b.getX() - runnable_mv.mouse_prev_displacement.getX())/Global.BALL_WEIGHT;
            dy = (float)(runnable_b.getY() - runnable_mv.mouse_prev_displacement.getY())/Global.BALL_WEIGHT;
            System.out.format("x_velocity=%f, y_velocity=%f\n", dx, dy);
        }
        
        private void applyFriction()
        {
            if(dx > 0)
            {
                dx -= Global.FRICTION;
            }
            if(dx < 0)
            {
                dx += Global.FRICTION;
            }
            if(Math.abs(dx) < Global.FRICTION)
            {
                dx = 0;
            }
        }
        
        private void applyGravity()
        {
            dy += Global.GRAVITY;
        }
        
        private void ballPhysicsMethod()
        {
            if(runnable_mv.calculate_velocity)
            {
                calculateVelocity();
                runnable_mv.calculate_velocity = false;
            }
            runnable_b.moveX(dx);
            runnable_b.moveY(dy);
            checkBounds();
            applyGravity();
        }
        
        private void tractorBeamMethod()
        {
        	calculateTractorBeamTrajectory();
            runnable_b.moveX(dx);
            runnable_b.moveY(dy);
            checkBounds();
        }
        
        private void trajectoryMethod()
        {
            Point2D.Float trajectP = new Point2D.Float(runnable_b.x, runnable_b.y);
            t.addPoint(trajectP);
        }
        
        public BallRunnable(Point initial_velocity_vector, MouseVelocity mv, BallLine b, Trajectory t)
        {
            dx = initial_velocity_vector.x;
            dy = initial_velocity_vector.y;
            wall_y_bound = new Bounds(0, Global.SCREEN_HEIGHT);
            wall_x_bound = new Bounds(0, Global.SCREEN_WIDTH);
            ball_run = new Thread(this);
            ball_run.start();
            runnable_mv = mv;
            runnable_b = b;
            runnable_t = t;
        }
        
        public void run()
        {
            while(true)
            {
                if(runnable_mv.start_physics)
                {
                	//System.out.println("Clearing Trajectory Array");
                	trajectoryMethod();
                	//t.clearTrajectoryArray();
                	ballPhysicsMethod();
                }
                else if(runnable_mv.start_tractor_beam)
                {
                	t.clearTrajectoryArray();
                	tractorBeamMethod();
                	//trajectoryMethod();
                	
                }
                else
                {
                    checkBounds();
                }
                repaint();
                try {
                    
                    Thread.sleep(Global.TIME_TICK);
                } catch (InterruptedException ex) {
                    
                    Logger.getLogger(BallAnimator.class.getName()).log(Level.SEVERE, 
                        null, ex);
                }
            }
        }
    }
    
    private void debugBallLocation()
    {
        System.out.format("ballX=%f,ballY=%f,ballWidth=%f,ballHeight=%f\n", 
                            my_ball.x, my_ball.y, my_ball.width, my_ball.height);
    }
    
    private void initBallAnimator()
    {
        Point ctr = new Point(Global.SCREEN_WIDTH/2 - (int)Global.DIAMETER/2, 
                Global.SCREEN_HEIGHT/2 - (int)Global.DIAMETER/2);
        my_ball = new BallLine(ctr.x, ctr.y, Global.DIAMETER, Global.DIAMETER);
        
        /*
        rLine = new RotatingLine(Global.SCREEN_WIDTH/2 - Global.DIAMETER/2,
                            Global.SCREEN_HEIGHT/2,
                            Global.SCREEN_WIDTH/2  + Global.DIAMETER/2,
                            Global.SCREEN_HEIGHT/2);
        */
        
        t = new Trajectory();
        mv = new MouseVelocity();
        md = new BallMouseDetector(mv, my_ball);
        rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        startBallRunnable(mv);
        this.addMouseListener(md);
        this.addMouseMotionListener(md);
    }
    
    private void drawBallLine(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        //g2d.draw(bCirc);
        g2d.setRenderingHints(rh);
        g2d.setColor(Global.B_COLOR);
        g2d.drawOval((int)my_ball.x, (int)my_ball.y, (int)my_ball.width, (int)my_ball.height);
        g2d.setColor(Global.B_COLOR);
        g2d.fillOval((int)my_ball.x, (int)my_ball.y, (int)my_ball.width, (int)my_ball.height);
        g2d.setColor(Global.L_COLOR);
        g2d.draw(my_ball.rLine);
        //g2d.draw(rLine);
    }
    
    public void startBallRunnable(MouseVelocity mv)
    {
        Point velocity_vector = new Point(Global.DX, Global.DY);
        b_run = new BallRunnable(velocity_vector, mv, my_ball, t);
    }
    
    public BallAnimator()
    {
        initBallAnimator();
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        drawBallLine(g);
        t.drawLastLine((Graphics2D)g);
        //debugBallLocation();
    }
    
    
    class BallMouseDetector implements MouseListener, MouseMotionListener
    {
        private boolean mouseReleased;
        private MouseVelocity detector_mv;
        private BallLine detector_ball;
        private Point2D.Float pointer_offset;
        
        public BallMouseDetector(MouseVelocity mv, BallLine b)
        {
            mouseReleased = true;
            detector_mv = mv;
            detector_ball = b;
            pointer_offset = new Point2D.Float(detector_ball.width/2, detector_ball.height/2);
        }
        
        private void setDetectorBallPos(MouseEvent e)
        {
            detector_ball.setPosY((float)(e.getY() - pointer_offset.getY()));
            detector_ball.setPosX((float)(e.getX() - pointer_offset.getX()));
        }
        
        /*Below two functions go together for press and release*/
        public void mousePressed(MouseEvent e) {
        	detector_mv.start_tractor_beam = true;
            mouseReleased = false;
            detector_mv.start_physics = false;
            detector_mv.setMousePrevDisplacement(e.getX(), e.getY());
            detector_mv.setMouseTractorBeamDisplacement(e.getX() - (float)pointer_offset.getX(), 
            											e.getY() - (float)pointer_offset.getY());
            //setDetectorBallPos(e);
            //repaint();
        }
        
        public void mouseReleased(MouseEvent e) 
        {
            if(!mouseReleased)
            {
            	if(detector_mv.start_tractor_beam)
            	{
            		detector_mv.start_tractor_beam = false;
                    detector_mv.start_physics = true;
                    mouseReleased = true;
            	}
            	else
            	{
                    detector_mv.calculate_velocity = true;
                    detector_mv.start_physics = true;
                    mouseReleased = true;
            	}

            }
            
        }
        
        /*These functions detect whether the mouse pointer have entered or exited a component*/
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
        
        /*Detects both a press and a release*/
        public void mouseClicked(MouseEvent e) { }
        
        /**/
        public void mouseMoved(MouseEvent e) { }
        public void mouseDragged(MouseEvent e) 
        {
        	if(!detector_mv.start_tractor_beam)
        	{
        		setDetectorBallPos(e);
        	}
        	else
        	{
        		detector_mv.setMouseTractorBeamDisplacement(e.getX(), e.getY());
        	}
        }
    }
    
    class MouseVelocity
    {
        public Point2D.Float mouse_prev_displacement;
        public Point2D.Float mouse_velocity;
        public Point2D.Float mouse_tractor_beam_displacement;
        public boolean start_physics;
        public boolean calculate_velocity;
        public boolean start_tractor_beam = false;
        
        MouseVelocity()
        {
            mouse_velocity = new Point2D.Float();
            mouse_prev_displacement = new Point2D.Float();
            mouse_tractor_beam_displacement = new Point2D.Float();
        }
        
        public void setMousePrevDisplacement(float x, float y)
        {
        	mouse_prev_displacement.x = x;
        	mouse_prev_displacement.y = y;
        }
        
        public void setMouseTractorBeamDisplacement(float x, float y)
        {
        	mouse_tractor_beam_displacement.x = x;
        	mouse_tractor_beam_displacement.y = y;
        }
        
        public Point2D.Float calculateMouseVelocity(Point2D.Float mouse_curr_dispalcement)
        {
            Point2D.Float velocity_vector = new Point2D.Float();
            if(mouse_prev_displacement != null)
            {
                velocity_vector.x = (float)(mouse_curr_dispalcement.getX() - mouse_prev_displacement.getX());
                velocity_vector.y = (float)(mouse_curr_dispalcement.getY() - mouse_prev_displacement.getY());
            }
            else
            {
                System.out.println("NULL_MOUSE_VELOCITY!");
                velocity_vector = null;
            }
            return velocity_vector;
        }
    }
}

public class BouncingBall extends JFrame {
    private BallAnimator s;
    
    /**
     * @param args
     */
    private Insets getFrameInsets()
    {
        /*Get the size of the border and store in Insets*/
        JFrame temp = new JFrame();
        temp.pack();
        Insets temp_insets = temp.getInsets();
        temp = null;
        return temp_insets;
    }
    
    public BouncingBall()
    {        
        /*Create JFrame*/
        Dimension dim = new Dimension(Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        this.setTitle("Hit testing");
        
        /*Add the ball object*/
        s = new BallAnimator();
        s.setPreferredSize(dim);
        s.setBackground(Global.BGROUND_COLOR);
        this.add(s, BorderLayout.CENTER);
        this.pack();
        
        /*Set the background, size and window change states*/
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                BouncingBall b = new BouncingBall();
                b.setVisible(true);
            }
        });     

    }
}