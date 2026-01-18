(ns io.github.tomboyo.lily.compiler.cg.string-template
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [stringify-keys]])
  (:import (org.stringtemplate.v4 STGroupFile StringRenderer)))

(def stgf (let [tmp (STGroupFile. (io/resource "templates/template.stg"))]
            (.registerRenderer tmp String (StringRenderer.))
            tmp))

(defn st-render [template m]
  (reduce
    (fn [result [k v]]
      (.add result (name k) (stringify-keys v)))
    (.getInstanceOf stgf template)
    m))

(defn record [m] (st-render "record" m))
(defn wither [m] (st-render "wither" m))
(defn empty-fn [m] (st-render "emptyFactory" m))
