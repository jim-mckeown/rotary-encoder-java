package Encoder;
/**
 * Determines the angle of the boundary between the black half and white half of
 * a disk. The camera should be as close to the surface as possible while 
 * maintaining view of as much of the active part of the disk as possible. The 
 * camera should be perpendicular to the disk.
 * <p>
 * The calculated angle is updated as fast as possible and is available through
 * the public getAngle() method and on server socket 8888 (default).
 * <p>
 * A video window is optionally displayed to aid in camera setup and 
 * troubleshooting. The location of the active area is indicated in the video
 * window by a green square. The calculated angle is displayed on the captured
 * image.
 * <p>
 * The captured video image has the origin in the upper left. X increases to the
 * right. Y increases down.
 * <p>
 *  
 * @author Jim McKeown
 */


import java.awt.Image;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.opencv.videoio.VideoCapture;



public class Encoder
{
    private int video; // 0, no video, 1, analog video, 2 digital video
    private double angle;
    int activeSize = 0; 
    private static int serverSocket = 8888;
    private EncServer encSvr;
    private int threshold = 128;
    
    /**
     * Test method. Runs this classes run() method. Set active area to 150 x 150
     * square at the center of the captured video. Show original video with
     * active area identified and calculated angle displayed.
     * @param args 
     */
    public static void main(String[] args) 
    {
        Encoder encoder = new Encoder(150, 1, serverSocket);
        encoder.run();
    }
    
    /**
     * Sole constructor. Instance the Encoder class with the option to display
     * the captured image.
     * 
     * @param activeSize int size of the active area of the captured video 
     * measured in pixels. Active area is activeSize x activeSize at center of
     * captured image. 

     * @param showVideo boolean flag used to optionally display the captured 
     * image, active area, and calculated angle.
     */
    
    public Encoder(int activeSize, int video, int serverSocket)
    {
        this.activeSize = activeSize;
        this.video = video;
        this.serverSocket = serverSocket;
    }
    
    /**
     * 
     * @return public access to the angle result in degrees counter-clockwise
     * from the x-axis. 
     * 
     */
    public double getAngle()
    {
        return angle;
    }
    public void setDisplay(int video)
    {
        this.video = video;
    }
    public int getThreshold()
    {
        return threshold;
    }
    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }
    public int getSize()
    {
        return activeSize;
    }
    public void setSize(int size)
    {
        this.activeSize = size;
    }
    /**
     * Thread entry point
     */
    //@Override
    public void run() 
    {
        //OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        encSvr = new EncServer(this, serverSocket);
        new Thread(encSvr).start();
        //capture a frame to image src
        Image src = null; //IplImage src = null;
        //IplImage show = null;
        //VideoInputFrameGrabber grabber = new VideoInputFrameGrabber(0);
        VideoCapture grabber = null;
        //OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
        try
        {
            try
            {
                //grabber = new OpenCVFrameGrabber(0);
                grabber.open(0);
                //grabber.init();
                
            }
            catch (Exception e)
            {
                System.out.println("FrameGrabber.createDefault(0) did not work. trying OpenCVFrameGrabber(0)");
                if (grabber != null) grabber.release();
                //grabber = FrameGrabber.createDefault(0);
                grabber = new OpenCVFrameGrabber(0);
            }
            //System.out.println("Calling grabber.start()");
            grabber.start();
            Thread.sleep(1);
            //System.out.println("Initial frame grab.");
            src = grabberConverter.convert(grabber.grab());
        }
        catch(FrameGrabber.Exception e)
        {
            System.out.println("Error...ending application.");
            System.out.println("FrameGrabber Exception: " + e);
            System.exit(0);           
        }
        catch(InterruptedException e)
        {
            
        }
        CanvasFrame srcFrame = null;
        
        //active area is activeSize x activeSize at center of captured image
        int centerX = src.width() / 2;
        int centerY = src.height() / 2;        
        int Xul = centerX - (activeSize / 2); //170 @ 300 activeSize
        int Yul = centerY - (activeSize / 2); // 90 @ 300 activeSize
        int Xlr = centerX + (activeSize / 2); //469 @ 300 activeSize
        int Ylr = centerY + (activeSize / 2); //379 @ 300 activeSize        

        if(video > 0)
        {
            srcFrame =  new CanvasFrame("Rotary Encoder");
            srcFrame.setDefaultCloseOperation(CanvasFrame.EXIT_ON_CLOSE);
            srcFrame.setAutoRequestFocus(false);
        }
        IplImage gray = cvCreateImage(cvGetSize(src), 8, 1);
        IplImage hsv = cvCreateImage(cvGetSize(src), 8, 3);
        CvScalar minColor = cvScalar(0, 0, threshold, 0); //detect white
        CvScalar maxColor = cvScalar(128, 128, 255, 0);
        ByteBuffer grayArray;
        //ByteBuffer byteBuffer;

        byte lastPixel = 0;
        byte currentPixel = 0;
        SimpleRegression siVHtL = new SimpleRegression();
        SimpleRegression siVLtH = new SimpleRegression();
        SimpleRegression siHHtL = new SimpleRegression();
        SimpleRegression siHLtH = new SimpleRegression();
        
        while(true)
        {
            //active area is activeSize x activeSize at center of captured image
            centerX = src.width() / 2;
            centerY = src.height() / 2;        
            Xul = centerX - (activeSize / 2); //170 @ 300 activeSize
            Yul = centerY - (activeSize / 2); // 90 @ 300 activeSize
            Xlr = centerX + (activeSize / 2); //469 @ 300 activeSize
            Ylr = centerY + (activeSize / 2); //379 @ 300 activeSize        

            //src = cvQueryFrame(capture);
            try
            {
                //System.out.print("grab..");
                src = grabberConverter.convert(grabber.grab());
            }
            catch(FrameGrabber.Exception e)
            {
                System.out.println("Error...ending application.");
                System.exit(0);  
            }
            cvCvtColor(src, hsv, CV_BGR2HSV);
            minColor = cvScalar(0, 0, threshold, 0);
            cvInRangeS(hsv, minColor, maxColor, gray);
            
            //convert grayscale image to ByteBuffer
            grayArray = gray.getByteBuffer();
            //grayArray = gray.asByteBuffer();
            //byteBuffer = gray.asByteBuffer();
            //byteBuffer.get(byteBuffer.limit() -1);

            //byteArray = gray.asByteBuffer().array();
            //scan the active area
            lastPixel = 0;
            currentPixel = 0;
            
            siVHtL.clear();
            siVLtH.clear();
            siHHtL.clear();
            siHLtH.clear();
            
            //vertical scan top to bottom
            for(int x = Xul;x < Xlr;x++)
            {
                lastPixel = grayArray.get((gray.width() * Yul) + x);
                for(int y = Yul;y < Ylr;y++)
                {
                    //detect high to low transition
                    currentPixel = grayArray.get((gray.width() * y) + x);
                    if(lastPixel == -1 && currentPixel == 0)
                    {
                        //add coordinates to vertical high to low array(045-315)
                        siVHtL.addData((double)x, (double)(0 - y));
                    }
                    if(lastPixel == 0 && currentPixel == -1)
                    {
                        //add coordinates to vertical low to high array(135-225)
                        siVLtH.addData((double)x, (double)(0 - y));
                    }
                    lastPixel = currentPixel;
                }
            }
            
            //horizontal scan left to right
            for(int y = Yul;y < Ylr;y++)
            {
                lastPixel = grayArray.get((gray.width() * y) + Xul);
                for(int x = Xul;x < Xlr;x++)
                {
                    //detect high to low transition
                    currentPixel = grayArray.get((gray.width() * y) + x);
                    if(lastPixel == -1 && currentPixel == 0)
                    {
                        //add coordinates to vertical high to low array(225-315)
                        siHHtL.addData((double)y, (double)x);
                    }
                    if(lastPixel == 0 && currentPixel == -1)
                    {
                        //add coordinates to vertical low to high array(045-135)
                        siHLtH.addData((double)y, (double)x);
                    }
                    lastPixel = currentPixel;
                }
            }
            
            //find highest N and calculate angle from slope
            if(siVHtL.getN() >= siVLtH.getN() && siVHtL.getN() >= siHLtH.getN() 
                    && siVHtL.getN() >= siHHtL.getN())
            {
                //angle = 135 to 225 degrees
                angle = 180 + Math.toDegrees(Math.atan(siVHtL.getSlope()));
                //System.out.println("Vertical High to Low");
            }
            if(siVLtH.getN() >= siVHtL.getN() && siVLtH.getN() >= siHLtH.getN() 
                    && siVLtH.getN() >= siHHtL.getN())
            {
                //angle = 315 to 360 degrees
                if(siVLtH.getSlope() < 0)
                {
                    angle = 360.0 + 
                            Math.toDegrees(Math.atan(siVLtH.getSlope()));
                    //System.out.println("Vertical Low to High 315 - 360");                    
                }
                else
                //angle = 000 to 045 degrees    
                {
                    angle = Math.toDegrees(Math.atan(siVLtH.getSlope()));
                    //System.out.println("Vertical Low to High 000 - 045");
                }                                
            }
            if(siHHtL.getN() >= siVLtH.getN() && siHHtL.getN() >= siVHtL.getN() 
                    && siHHtL.getN() >= siHLtH.getN())
            {
                //angle = 225 to 315 degrees
                angle = 270.0 + Math.toDegrees(Math.atan(siHHtL.getSlope()));
                //System.out.println("Horizontal High to Low");
            }
            if(siHLtH.getN() >= siVLtH.getN() && siHLtH.getN() >= siVHtL.getN() 
                    && siHLtH.getN() >= siHHtL.getN())
            {
                //angle = 45 to 135 degrees
                angle = 90.0 + Math.toDegrees(Math.atan(siHLtH.getSlope()));
                //System.out.println("Horizontal Low to High");
            }
            
            if(video > 0)
            {
                if(!srcFrame.isVisible()) srcFrame.setVisible(true);
                if(video == 2)
                {
                    cvCvtColor(gray, src, CV_GRAY2BGR);
                }
            
                //draw green square around active area
                cvRectangle(src, cvPoint(Xul, Yul), cvPoint(Xlr, Ylr), 
                        CvScalar.GREEN, 1, 0, 0);
                //draw green crosshairs
                cvLine(src, cvPoint(0, centerY), cvPoint(Xul, centerY), 
                        CvScalar.GREEN, 1, 0, 0);
                cvLine(src, cvPoint(centerX, 0), cvPoint(centerX, Yul), 
                        CvScalar.GREEN, 1, 0, 0);
                cvLine(src, cvPoint(Xlr, centerY), cvPoint((src.width() - 1),
                        centerY), CvScalar.GREEN, 1, 0, 0);
                cvLine(src, cvPoint(centerX, Ylr), cvPoint(centerX, 
                        (src.height() - 1)), CvScalar.GREEN, 1, 0, 0);

                String formatedAngle = 
                        new DecimalFormat("000.00").format(angle);
                CvFont font = new CvFont(); 
                cvInitFont(font, CV_FONT_HERSHEY_DUPLEX, 2.0, 2.0,0.2, 2, 8);
                cvPutText(src, formatedAngle, cvPoint(10,50),
                        font, CvScalar.RED); 
                
                
                srcFrame.showImage(converter.convert(src)); //srcFrame.showImage(src);
            }
            else
            {
                if(srcFrame.isVisible()) srcFrame.setVisible(false);
            }
        }
    }
}
