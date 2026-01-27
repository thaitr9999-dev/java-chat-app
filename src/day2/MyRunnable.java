package day2 ; 
public class MyRunnable implements Runnable {
    private String threadName ;

    public MyRunnable(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public void run() {
        System.out.println(threadName + " is running.");

        for (
             int i = 1; i < 3; i++) {
                System.out.println(threadName + ": Message " + i);
                try {
                    Thread.sleep(500); // Sleep for 500 milliseconds
                } catch (InterruptedException e) {
                    System.out.println(threadName + " interrupted.");
                }

        }

    }
}
