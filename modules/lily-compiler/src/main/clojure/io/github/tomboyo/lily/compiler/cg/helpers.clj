(ns io.github.tomboyo.lily.compiler.cg.helpers
  (:require [clojure.java.io :as io])
  (:import (io.github.tomboyo.lily.compiler.cg Mustache)))

(def recordTemplate (slurp (io/resource "templates/record.java.mustache")))

(defprotocol Render
  (render [x]))

(defrecord Record [type]
  Render
  (render [record]
    (Mustache/writeString
      recordTemplate
      (str ::record)
      {"package"    (get-in record [:type :package])
       "class-name" (get-in record [:type :name])})))

(comment
  (render (map->Record {:type {:package "com.example" :name "Foo"}})))