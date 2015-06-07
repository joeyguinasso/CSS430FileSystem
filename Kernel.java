import java.util.*;
import java.lang.reflect.*;
import java.io.*;

public class Kernel
{
   // Interrupt requests
   public final static int INTERRUPT_SOFTWARE = 1;  // System calls
   public final static int INTERRUPT_DISK     = 2;  // Disk interrupts
   public final static int INTERRUPT_IO       = 3;  // Other I/O interrupts

   // System calls
   public final static int BOOT    =  0; // SysLib.boot( )
   public final static int EXEC    =  1; // SysLib.exec(String args[])
   public final static int WAIT    =  2; // SysLib.join( )
   public final static int EXIT    =  3; // SysLib.exit( )
   public final static int SLEEP   =  4; // SysLib.sleep(int milliseconds)
   public final static int RAWREAD =  5; // SysLib.rawread(int blk, byte b[])
   public final static int RAWWRITE=  6; // SysLib.rawwrite(int blk, byte b[])
   public final static int SYNC    =  7; // SysLib.sync( )
   public final static int READ    =  8; // SysLib.cin( )
   public final static int WRITE   =  9; // SysLib.cout( ) and SysLib.cerr( )

   // System calls to be added in Assignment 4
   public final static int CREAD   = 10; // SysLib.cread(int blk, byte b[])
   public final static int CWRITE  = 11; // SysLib.cwrite(int blk, byte b[])
   public final static int CSYNC   = 12; // SysLib.csync( )
   public final static int CFLUSH  = 13; // SysLib.cflush( )

   // System calls to be added in Project
   public final static int OPEN    = 14; // SysLib.open( String fileName )
   public final static int CLOSE   = 15; // SysLib.close( int fd )
   public final static int SIZE    = 16; // SysLib.size( int fd )
   public final static int SEEK    = 17; // SysLib.seek( int fd, int offest, 
   //              int whence )
   public final static int FORMAT  = 18; // SysLib.format( int files )
   public final static int DELETE  = 19; // SysLib.delete( String fileName )

   // Predefined file descriptors
   public final static int STDIN  = 0;
   public final static int STDOUT = 1;
   public final static int STDERR = 2;

   // Return values
   public final static int OK = 0;
   public final static int ERROR = -1;

   // System thread references
   private static Scheduler scheduler;
   private static Disk disk;
   private static Cache cache;
   private static FileSystem fs;

   // Synchronized Queues
   private static SyncQueue waitQueue;  // for threads to wait for their child
   private static SyncQueue ioQueue;    // I/O queue

   private final static int COND_DISK_REQ = 1; // wait condition 
   private final static int COND_DISK_FIN = 2; // wait condition

   // Standard input
   private static BufferedReader input
      = new BufferedReader( new InputStreamReader( System.in ) );

   // The heart of Kernel
   public static int interrupt(int irq, int cmd, int param, Object args) {
		TCB myTcb;
		int myTid, myPid;
		FileTableEntry myFte;
		switch (irq) {
			case INTERRUPT_SOFTWARE : // System calls
				switch (cmd) {
					case BOOT :
						// instantiate and start a scheduler
						scheduler = new Scheduler();
						scheduler.start();

						// instantiate and start a disk
						disk = new Disk(1000);
						disk.start();

						// instantiate a cache memory
						cache = new Cache(Disk.blockSize, 10);

						// instantiate synchronized queues
						ioQueue = new SyncQueue();
						waitQueue = new SyncQueue(scheduler.getMaxThreads());

						// Disk is ready, signal the ioQueue
						ioQueue.dequeueAndWakeup(COND_DISK_REQ);

						// instantiate a file system;
						fs = new FileSystem(1000);

						return OK;
					case EXEC :
						return sysExec((String[]) args);
					case WAIT :
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						myTid = myTcb.getTid(); // get my thread ID
						return waitQueue.enqueueAndSleep(myTid); 
						// wait on my tid
						// woken up by my child thread
					case EXIT :
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						myTid = myTcb.getTid(); // get my ID
						if ((myPid = myTcb.getPid()) == -1)
							return ERROR;
						// wake up a thread waiting on my parent ID
						waitQueue.dequeueAndWakeup(myPid, myTid);
						// I'm terminated!
						scheduler.deleteThread();
						return OK;
					case SLEEP : // sleep a given period of milliseconds
						scheduler.sleepThread(param); // param = milliseconds
						return OK;
					case RAWREAD : // read a block of data from disk
						ioQueue.enqueueAndSleep(COND_DISK_REQ);
						if (!disk.read(param, (byte[]) args))
						ioQueue.enqueueAndSleep(COND_DISK_FIN);
						disk.testAndResetReady();
						ioQueue.dequeueAndWakeup(COND_DISK_REQ);

						return OK;
					case RAWWRITE : // write a block of data to disk
						ioQueue.enqueueAndSleep(COND_DISK_REQ);
						if (!disk.write(param, (byte[]) args))
						ioQueue.enqueueAndSleep(COND_DISK_FIN);
						disk.testAndResetReady();
						ioQueue.dequeueAndWakeup(COND_DISK_REQ);

						return OK;
					case SYNC : // synchronize disk data to a real file
						fs.sync();
						ioQueue.enqueueAndSleep(COND_DISK_REQ);
						if (!disk.sync())
						ioQueue.enqueueAndSleep(COND_DISK_FIN);
						disk.testAndResetReady();
						ioQueue.dequeueAndWakeup(COND_DISK_REQ);
						return OK;
					case READ :
						switch (param) {
							case STDIN :
								try {
									String s; // read a keyboard input
									if ((s = input.readLine()) == null)
										return ERROR;
									// prepare a read buffer
									StringBuffer buf = (StringBuffer) args;

									// append the keyboard input to this read
									// buffer
									buf.append(s);

									// return the number of chars read from
									// keyboard
									return s.length();
								} catch (IOException e) {
									return ERROR;
								}
							case STDOUT :
							case STDERR :
								return ERROR;
						}
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = myTcb.getFtEnt(param)) == null)
							return ERROR;
						return fs.read(myFte, (byte[]) args);
					case WRITE :
						switch (param) {
							case STDIN :
								return ERROR;
							case STDOUT :
								System.out.print(args);
								return OK;
							case STDERR :
								System.err.print(args);
								return OK;
						}
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = myTcb.getFtEnt(param)) == null)
							return ERROR;
						return fs.write(myFte, (byte[]) args);
					case CREAD :
						return cache.read(param, (byte[]) args) ? OK : ERROR;
					case CWRITE :
						return cache.write(param, (byte[]) args) ? OK : ERROR;
					case CSYNC :
						cache.sync();
						return OK;
					case CFLUSH :
						cache.flush();
						return OK;
					case OPEN :
						String[] openArgs = (String[]) args;
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = fs.open(openArgs[0], openArgs[1])) == null)
							return ERROR;
						return myTcb.getFd(myFte);
					case CLOSE :
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = myTcb.getFtEnt(param)) == null
								|| fs.close(myFte) == false)
							return ERROR;
						if (myTcb.returnFd(param) != myFte)
							return ERROR;
						return OK;
					case SIZE :
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = myTcb.getFtEnt(param)) == null)
							return ERROR;
						return fs.fsize(myFte);
					case SEEK :
						int[] seekArgs = (int[]) args;
						if ((myTcb = scheduler.getMyTcb()) == null)
							return ERROR;
						if ((myFte = myTcb.getFtEnt(param)) == null)
							return ERROR;
						return fs.seek(myFte, seekArgs[0], seekArgs[1]);
					case FORMAT :
						return (fs.format(param) == true) ? OK : ERROR;
					case DELETE :
						return (fs.delete((String) args) == true) ? OK : ERROR;
				}
				return ERROR;
			case INTERRUPT_DISK : // Disk interrupts
				// wake up the thread waiting for a service completion
				ioQueue.dequeueAndWakeup(COND_DISK_FIN);
				return OK;
			case INTERRUPT_IO : // other I/O interrupts (not implemented)
				return OK;
		}
		return OK;
	}

   // Spawning a new thread
   private static int sysExec( String args[] ) {
      String thrName = args[0]; // args[0] has a thread name
      Object thrObj = null;

      try {
         //get the user thread class from its name
         Class thrClass = Class.forName( thrName ); 
         if ( args.length == 1 ) // no arguments
            thrObj = thrClass.newInstance( ); // instantiate this class obj
         else {                  // some arguments
            // copy all arguments into thrArgs[] and make a new constructor
            // argument object from thrArgs[]
            String thrArgs[] = new String[ args.length - 1 ];
            for ( int i = 1; i < args.length; i++ ){
               thrArgs[i - 1] = args[i];
			}
            Object[] constructorArgs = new Object[] { thrArgs };

            // locate this class object's constructors
            Constructor thrConst 
               = thrClass.getConstructor( new Class[] {String[].class} );

            // instantiate this class object by calling this constructor
            // with arguments
            thrObj = thrConst.newInstance( constructorArgs );
         }
         // instantiate a new thread of this object
         Thread t = new Thread( (Runnable)thrObj );

         // add this thread into scheduler's circular list.
         TCB newTcb = scheduler.addThread( t );
         return ( newTcb != null ) ? newTcb.getTid( ) : ERROR;
      }
      catch ( ClassNotFoundException e ) {
         System.out.println( e );
         return ERROR;
      }
      catch ( NoSuchMethodException e ) {
         System.out.println( e );
         return ERROR;
      }
      catch ( InstantiationException e ) {
         System.out.println( e );
         return ERROR;
      }
      catch ( IllegalAccessException e ) {
         System.out.println( e );
         return ERROR;
      }
      catch ( InvocationTargetException e ) {
         System.out.println( e );
         return ERROR;
      }
   }
}
