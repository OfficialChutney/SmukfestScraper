package org.dataoganalysekaeder;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scraper {

    private String lastUser;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Scraper() {
        // Load previously searched usernames
        try (BufferedReader reader = new BufferedReader(new FileReader("alreadySearched.txt"))) {
            String username;
            lastUser = reader.readLine();

            if(lastUser == null) {
                lastUser = "null";
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
        System.out.println("Type 'stop' to terminate scraping");
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

            System.out.println("Found post by user: " + username);

            if (!lastUser.equals(username)) {
                lastUser = username;

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("alreadySearched.txt"))) {
                    writer.write(username);
                }


                WebElement bottom = post.findElement(By.cssSelector("a.x1i10hfl.x1ypdohk.xdl72j9.x2lah0s.xe8uvvx.xdj266r.x14z9mp.xat24cr.x1lziwak.xeuugli.x16tdsg8.xggy1nq.x1ja2u2z.x1t137rt.x1fmog5m.xu25z0z.x140muxe.xo1y3bh.x87ps6o.x1lku1pv.x1a2a7pz.x6s0dn4.xmjcpbm.xso031l.x972fbf.x1exxf4d.xpv9jar.x1nb4dca.x1nmn18.x10w94by.x14e42zd.x13fuv20.x18b5jzi.x1q0q8m5.x1t7ytsu.x9f619.x78zum5.x1q0g3np.x1nhvcw1.x1wxaq2x.xz9dl7a.xsag5q8.xv54qhq.xf7dkkf.x1n2onr6.x1hl2dhg"));

                String titel = bottom.findElement(By.cssSelector("div.xu06os2.x1ok221b span.x193iq5w.xeuugli.x13faqbe.x1vvkbs.x1xmvt09.x1lliihq.x1s928wv.xhkezso.x1gmr53x.x1cpjm7i.x1fgarty.x1943h6x.xudqn12.x3x7a5m.x1lkfr7t.x1lbecb7.x1s688f.xzsf02u.x1yc453h span div")).getText().toLowerCase();

                if (titel.contains("bytte")) {
                    continue;
                }

                if (!titel.contains("sælg")) {
                    continue;
                }

                if (titel.contains("søger")) {
                    continue;
                }

                if (!(titel.contains("partout")
                        || (titel.contains("plads") && titel.contains("kærligheden")))) {
                    continue;
                }

                System.out.println("Post titel contains keywords");

                WebElement sendMessageButton = bottom.findElement(By.cssSelector("div[role='button']"));

                sendMessageButton.click();

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement textarea = wait.until(
                        ExpectedConditions.elementToBeClickable(By.cssSelector("textarea"))
                );

                textarea.sendKeys("Hvis det stadig er til salg, vil jeg gerne købe det! :)");


                WebElement dialogBox = driver.findElement(
                        By.cssSelector("[aria-label*='Send en besked til']")
                );

                WebElement sendButton = dialogBox.findElement(By.cssSelector("[aria-label*='Send besked']"));

                sendButton.click();

                System.out.println("Purchase message sent.");

                Thread.sleep(5000);

                driver.navigate().refresh();

            } else {
                System.out.println("Already searched post by: " + username + ". Refreshing...");
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
