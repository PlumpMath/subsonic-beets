(ns subsonic-beets.util)


(defn tprint
  "Transparent print. Do a println and return the value being printed."
  [retval] (doto retval println))
