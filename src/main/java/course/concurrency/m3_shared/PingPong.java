package course.concurrency.m3_shared;

public class PingPong {

    final static Ball ball = new Ball();

    public static void ping() {
        racket(true);
    }

    public static void pong() {
        racket(false);
    }

    private static void racket(boolean isPing) {
        try {
            for (int i = 0; i <3 ; i++){
                synchronized (ball) {
                    while (isPing==ball.isPing()) ball.wait();
                    ball.setPing(isPing);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> ping());
        Thread t2 = new Thread(() -> pong());
        t1.start();
        t2.start();
    }

    private static class Ball {
        private boolean ping = false;

        public boolean isPing() {
            return ping;
        }

        public void setPing(boolean ping) {
            this.ping = ping;
            System.out.println(this.ping?"ping":"pong");
            this.notify();
        }
    }

}
