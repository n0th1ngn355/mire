(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *name*)
(def hp (atom 50))
(def max-hp (atom 100))
(def current-chest (atom nil))

(defn health-bar []
  (let [health-per-block (/ @max-hp 10)
        blocks (int (/ @hp health-per-block))]
    (apply str (concat ["HP: ["] (repeat blocks "█") (repeat (- 10 blocks) "░") ["]"]))))


(def prompt "> ")
(def streams (ref {}))

(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))
