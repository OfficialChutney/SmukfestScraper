package org.dataoganalysekaeder;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scraper {

    private final Set<String> alreadySearchedUsers = new HashSet<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Scraper() {
        // Load previously searched usernames
        try (BufferedReader reader = new BufferedReader(new FileReader("alreadySearched.txt"))) {
            String username;
            while ((username = reader.readLine()) != null) {
                alreadySearchedUsers.add(username);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load alreadySearched.txt", e);
        }
    }

    public void scrape() throws Exception {
        System.setProperty("webdriver.chrome.driver", "./driver/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        // Navigate and apply cookies
        driver.get("https://www.facebook.com/groups/1060326807352660/?sorting_setting=CHRONOLOGICAL_LISTINGS");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cookies.data"))) {
            @SuppressWarnings("unchecked")

            Set<Cookie> cookies = (Set<Cookie>) ois.readObject();
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
        }

        Thread.sleep(500);

        driver.navigate().refresh();

        Scanner console = new Scanner(System.in);

        System.out.println("Type 'start' and press Enter to begin scraping...");
        while (console.hasNextLine()) {
            String cmd = console.nextLine().trim();
            if ("start".equalsIgnoreCase(cmd)) {
                running.set(true);
                break;
            }
        }

        Thread stopper = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (running.get()) {
                String line = sc.nextLine().trim();
                if ("stop".equalsIgnoreCase(line)) {
                    running.set(false);
                    System.out.println("Stop signal received. Shutting down...");
                }
            }
        });
        stopper.setDaemon(true);
        stopper.start();

        while (running.get()) {

            WebElement feed = driver.findElement(By.cssSelector("div[role='feed']"));

            WebElement post = feed.findElement(By.cssSelector("div.x1yztbdb.x1n2onr6.xh8yej3.x1ja2u2z"));

            WebElement userSpan = post.findElement(
                    By.cssSelector("span.html-span.xdj266r.x14z9mp.xat24cr.x1lziwak.xexx8yu.xyri2b.x18d9i69.x1c1uobl.x1hl2dhg.x16tdsg8.x1vvkbs")
            );

            String username = userSpan.getText().toLowerCase();
            System.out.println("Found user: " + username);

            if (!alreadySearchedUsers.contains(username)) {
                alreadySearchedUsers.add(username);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("alreadySearched.txt", true))) {
                    writer.write(username);
                    writer.newLine();
                }


                String titel = post.findElement(By.cssSelector("span.x1lliihq.x6ikm8r.x10wlt62.x1n2onr6.x1j85h84 div"));

                System.out.println(titel);

            } else {
                System.out.println("Already searched: " + username + ". Refreshing...");
                driver.navigate().refresh();
            }

            // small delay to avoid hammering
            Thread.sleep(2000);
        }

        // Cleanup
        System.out.println("Scraping stopped.");
        driver.quit();
    }

    public static void main(String[] args) {
        try {
            new Scraper().scrape();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
