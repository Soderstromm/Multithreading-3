import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final String END_OF_QUEUE = "END"; // Маркер завершения
    private static final BlockingQueue<String> queueA = new ArrayBlockingQueue<>(100);
    private static final BlockingQueue<String> queueB = new ArrayBlockingQueue<>(100);
    private static final BlockingQueue<String> queueC = new ArrayBlockingQueue<>(100);

    // Максимальные значения для каждого символа
    private static final AtomicInteger maxA = new AtomicInteger(0);
    private static final AtomicInteger maxB = new AtomicInteger(0);
    private static final AtomicInteger maxC = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        int totalTexts = 10_000;
        int textLength = 100_000;
        String letters = "abc";

        // Поток-генератор текстов
        Thread generator = new Thread(() -> {
            try {
                for (int i = 0; i < totalTexts; i++) {
                    String text = generateText(letters, textLength);
                    queueA.put(text);
                    queueB.put(text);
                    queueC.put(text);
                }
                // Добавляем маркеры завершения в каждую очередь
                queueA.put(END_OF_QUEUE);
                queueB.put(END_OF_QUEUE);
                queueC.put(END_OF_QUEUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Потоки-анализаторы
        Thread analyzerA = createAnalyzerThread(queueA, 'a', maxA);
        Thread analyzerB = createAnalyzerThread(queueB, 'b', maxB);
        Thread analyzerC = createAnalyzerThread(queueC, 'c', maxC);

        // Запуск всех потоков
        generator.start();
        analyzerA.start();
        analyzerB.start();
        analyzerC.start();

        // Ожидание завершения
        generator.join();
        analyzerA.join();
        analyzerB.join();
        analyzerC.join();

        // Вывод результатов
        System.out.println("Максимальное количество 'a': " + maxA.get());
        System.out.println("Максимальное количество 'b': " + maxB.get());
        System.out.println("Максимальное количество 'c': " + maxC.get());
    }

    private static Thread createAnalyzerThread(BlockingQueue<String> queue, char target, AtomicInteger maxCounter) {
        return new Thread(() -> {
            try {
                while (true) {
                    String text = queue.take();
                    if (text.equals(END_OF_QUEUE)) break; // Выход при получении маркера
                    int count = countChar(text, target);
                    while (true) {
                        int currentMax = maxCounter.get();
                        if (count <= currentMax || maxCounter.compareAndSet(currentMax, count)) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    private static int countChar(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}