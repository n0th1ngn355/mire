(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.chests :as chests]
            [mire.player :as player]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

(defn- remove-from-ref
  "Remove one instance of obj from ref. Must call in a transaction."
  [obj from]
  (alter from disj obj))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (if-let [current-chest-name @player/current-chest]
    (let [chest (@chests/chests current-chest-name)] 
      (str "You are looking inside the " (:name @chest) "\n"
           (str/join "\n" (map #(str "There is " % " here.\n")
                               @(:items @chest)))))
    (str (:desc @player/*current-room*)
       "\nExits: " (keys @(:exits @player/*current-room*)) "\n"
       (str/join "\n" (map #(str "There is " % " here.\n")
                           @(:items @player/*current-room*)))
       (str/join "\n" (map #(str "There is " % " here.\n")
                           @(:chests @player/*current-room*)))
       ))
  )

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @player/*current-room*) (keyword direction))
         target (@rooms/rooms target-name)]
     (if target
       (do
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants target))
         (ref-set player/*current-room* target)
         (look))
       "You can't go that way."))))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (not (nil? @player/current-chest)) ; Проверяем, находится ли игрок в сундуке
     (let [chest (@chests/chests @player/current-chest)]
       (if (chests/chest-contains? @chest thing)
         (do (move-between-refs (keyword thing)
                                (:items @chest)
                                player/*inventory*)
             (str "You picked up the " thing "."))
         (str "There isn't any " thing " here.")))
     (if (rooms/room-contains? @player/*current-room* thing) ; Проверяем, находится ли игрок в комнате
       (do (move-between-refs (keyword thing)
                              (:items @player/*current-room*)
                              player/*inventory*)
           (str "You picked up the " thing "."))
       (str "There isn't any " thing " here.")))))


(defn- chest_items
  "See what's in the chest"
  [chest]
  (str "Chest contains:\n"
       (str/join "\n" (seq @(:items chest)))))

(defn- open_chest [chest]
  (swap! chest #(assoc % :is_closed false))
  (reset! player/current-chest (:name @chest))
  ""
  )

(defn back
  "Close the current chest."
  []
  (reset! player/current-chest nil)
  (look)
  )

(defn open
  "Open chest"
  [chest-name]
  (dosync
   (if (not ((:chests @player/*current-room*) (keyword chest-name)))
     (str "There is no chest with that name in this room.")  
     (let [chest (@chests/chests (keyword chest-name))]
       (if (:is_closed @chest)
         (if (@player/*inventory* :keys) 
           (do 
             (println "You use a keys to open the chest.")
             (println (chest_items @chest)) 
             (open_chest chest)
             (remove-from-ref :keys
                              player/*inventory*)
             ""
             ) 
           (str "The chest is closed, and you don't have any keys to open it.")) 
         (do 
           (reset! player/current-chest (:name @chest)) 
           (print (look))
           ""
           )
         )
       )
     )
   )
  )

;; (defn print_chests []
;;   (
;;     (println "List of all chests:")
;;     (doseq [name @chests/chests]
;;       (println "- " name))))

(defn print_chests
  "Chests"
  []
  (str (str/join "\n" (map #(str "There is " (first %) " here.\n") @chests/chests))))

(defn activate
  "Use item"
  [thing]
  (dosync
   (if (player/carrying? thing) 
     (cond 
       (= (keyword thing) :hp-potion)
       (if (>= @player/hp @player/max-hp)
         (str "You are already at full health.")
         (do
           (println "You use an HP potion.")
           (reset! player/hp (min @player/max-hp (+ @player/hp 50)))
           (remove-from-ref (keyword thing)
                              player/*inventory*)
           ""
           )
         )
       (= (keyword thing) :another-item) 
       (str "You use another item.")) 
     (str "You don't have that item."))))


(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\n"
       (str/join "\n" (seq @player/*inventory*))))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str/join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "open" open
               "chests" print_chests
               "use" activate
               "say" say
               "back" back
               "help" help})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         (apply (commands command) args))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
