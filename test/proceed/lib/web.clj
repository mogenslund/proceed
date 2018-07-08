(ns proceed.lib.web
  (:require [proceed.lib.util :as util]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [org.openqa.selenium.chrome ChromeDriver ChromeOptions]
           [org.openqa.selenium By WebDriver WebElement JavascriptExecutor Dimension]
           [org.openqa.selenium.remote DesiredCapabilities CapabilityType]
           [org.openqa.selenium.logging LoggingPreferences LogType]
           [org.openqa.selenium.remote RemoteWebDriver]
           [org.openqa.selenium.interactions Actions]
           [org.openqa.selenium.support.ui WebDriverWait ExpectedConditions]
           [java.util.concurrent TimeUnit]
           [java.util.logging Level Logger]
           [java.net.URL]))

(def myout (java.io.StringWriter.))

(def browsers (atom []))
(def retries (atom 30))

(defn next-browser
  "Use next browser"
  []
  (util/log "(next-browser)")
  (swap! browsers #(conj (subvec % 1) (first %))))

(defn web
  "Retrieve the first browser in the list."
  []
  (first @browsers))

(defn set-retries
  "Set how many times elements should be attempted to be
  found. Waiting one second between attempts."
  [sec]
  (util/log "(set-retries" sec ")")
  (reset! retries sec))

(defn new-browser
  "Start a new instance of Chrome."
  []
  (util/log "(new-browser)")
  (System/setProperty "webdriver.chrome.silentOutput", "true")
  (.setLevel (Logger/getLogger "org.openqa.selenium.remote") Level/OFF)
  (let [options (doto (ChromeOptions.)
                  (.addArguments ["--use-fake-ui-for-media-stream"
                                 ; "--headless"
                                 ; "--use-file-for-fake-audio-capture=/tmp/output.wav"
                                 ]))]
    (swap! browsers conj (ChromeDriver. options)))
    (-> (web) .manage .window (.setSize (Dimension. 1400 900))))

(defn sleep ; time in milliseconds
  "Sleep in milliseconds"
  [s]
  (Thread/sleep s))

(defn quit
  "Closes the browser instance."
  []
  (util/log "(quit)")
  (.quit (web))
  (swap! browsers subvec 1))

(defn quit-all
  "Close all browser instances."
  []
  (while (> (count @browsers) 0)
    (quit)))

(defn goto
  "Opens url in started Firefox.
  Prepends http:// if not starting with http."
  [url]
  (util/log (str "(goto \"" url "\")"))
  (if (.startsWith url "http")
    (.get (web) url)
    (.get (web) (str "http://" url))))

(defn get-url
  "Get the url of the current page."
  [web]
  (.getCurrentUrl web))

(defn get-elements
  [xpath]
  (let [elements (.findElements (web) (By/xpath (if (re-find #"/" xpath) xpath (str "//*[@id='" xpath "']"))))
        matches (filter #(.isDisplayed %) elements)]
    matches))

(defn get-element
  "Returns WebElement if present, otherwise nil.
  If the xpath look like an xpath xpath identifier
  is used, otherwise id."
  [xpath]
  (let [elements (.findElements (web) (By/xpath (if (re-find #"/" xpath) xpath (str "//*[@id='" xpath "']"))))
        matches (filter #(.isDisplayed %) elements)]
    (if (= (count matches) 0) ;; If there are no matches return nil
      nil
      (first matches))))      ;; Otherwise return the first element

(defn get-safe
  "Tries up to retries times to
  get the xpath. If no succes
  nil is returned.
  If the xpath is already an
  element, it is returned."
  [xpath]
  (if (string? xpath)
    (loop [i 0 e nil]
      (if (and (< i @retries) (not e))
        (do
          (sleep (if (= i 0) 0 1000))
          (recur (inc i) (get-element xpath)))
        (if e ;; If i>=10 or e not nil
          e
          (do
            (println (str "\n\nElement: \"" xpath "\" not found!\n\n"))
            false))))
    xpath))

(defn wait-clickable
  [element]
  (let [wait (WebDriverWait. (web) 30000)]
    (.until wait (ExpectedConditions/elementToBeClickable element))))

(defn wait-visible
  [element]
  (let [wait (WebDriverWait. (web) 30000)]
    (.until wait (ExpectedConditions/elementToBeClickable element))))

(defn wait-loading
  []
  (let [wait (WebDriverWait. (web) 30000)]
    (.until
      wait
        (ExpectedConditions/not
          (ExpectedConditions/presenceOfAllElementsLocatedBy
            (By/xpath "//div[@class='loading-spinner']"))))))

(defn default-wait
  []
  (sleep 500))

(defn click
  "Clicks a webelement.
  If the element is a string it will be
  considered an xpath."
  [element]
  (util/log (str "(click \"" element "\")"))
  (if-let [elem (get-safe element)]
    (do
      ;;<div class="loading-spinner">
      (wait-visible elem)
      (wait-clickable elem)
      (wait-loading)
      (default-wait)
      (.click (get-safe element)))
    false))

(defn click-relative
  "Clicks a webelement.
  If the element is a string it will be
  considered an xpath."
  [element left top]
  (util/log (str "(click-relative \"" element "\" " left " " top ")"))
  (if-let [elem (get-safe element)]
    (do
      ;;<div class="loading-spinner">
      (wait-visible elem)
      (wait-clickable elem)
      (wait-loading)
      (default-wait)
      (.perform (.build (.click (.moveToElement (Actions. (web)) (get-safe element) left top)))))
    false))

(defn send-keys
  "Takes a WebElement and a string to type into element.
  If the element is a string it will be considered an
  xpath."
  [element keys]
  (util/log (str "(send-keys \"" element "\" \"" keys "\")"))
  (if-let [elem (get-safe element)]
    (do
      (wait-visible elem)
      (wait-clickable elem)
      (wait-loading)
      (default-wait)
      (.sendKeys (get-safe element) (into-array [keys])))
    false))

(defn get-text
  "Returns the text content of a WebElement given
  the WebElement itself or an xpath to the WebElement."
  [element]
  (if-let [elem (get-safe element)]
    (.getText elem)
    false))

(defn clearfield
  "Clear the field with the given
  locator."
  [element]
  (if-let [elem (get-safe element)]
    (.clear elem)
    false))

(defn wait-for-elements
  "Wait for all elements to be awailable at the
  same time."
  [& elements]
  (loop [n 0]
    (when (= n @retries)
      (println (str "\n\nElements: " (str/join ", " (map #(str "\"" % "\"") elements)) " not found!\n\n"))
      false)
    (if (every? identity (map get-element elements))
      true
      (do (sleep 1000)
        (recur (inc n))))))

(defn hover
  "Hover the WebElement."
  [element]
  (let [elem (if (string? element) (get-safe element) element)]
    (-> (Actions. (web))
      (.moveToElement elem)
      (.build)
      (.perform))))

(defn switch-iframe
  "Switch to iframe with the given id."
  [id]
  (.frame (.switchTo (web)) id))

(defn testit
  []
  (set-retries 6)
  (new-browser)
  ;(sleep 100)
  (goto "http://wikipedia.org")
  (click "js-link-box-en")
  (send-keys "//input[@type='search']" "Turtle\n")
  ;(hover "p-cactions")
  (is (= (get-text "//h1") "Turtle") "Text not found")
  (wait-for-elements "//h1" "//input[@type='search']")
  (quit)
  ;(sleep 200)
  )

(defn testsequence
  []
  (try
    (set-retries 6)
    (new-browser)
    (goto "https://www.walmart.com/")
    (send-keys "global-search-input" "iphone 6s\n")
    (click "//img[contains(@alt,'Apple iPhone 6s')]")
    (is (= (get-text "//button[contains(@data-tl-id,'add_to_cart_button')]") "Add to Cart") "Text not found")
    (println "--DONE--")
    (quit)
    (catch Exception e (quit-all) (println e))))

;(doseq [x (range 6)] (testsequence))

;(doseq [x (range 10)] (testit))


; Chromedriver needs to be installed
;{:deps {org.seleniumhq.selenium/selenium-chrome-driver {:mvn/version "3.9.1"}
;        org.seleniumhq.selenium/selenium-support {:mvn/version "3.9.1"}}
; :paths ["/home/mogens/m/lib2"]}