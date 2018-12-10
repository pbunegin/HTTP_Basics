public class Main {
    private static final String URL = "https://yandex.ru/maps";

    public static void main(String[] args) {
        HttpApp httpApp = new HttpApp(URL);
        httpApp.start();
    }
}