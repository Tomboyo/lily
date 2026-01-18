(ns io.github.tomboyo.lily.compiler.cg.string-template
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [stringify-keys]])
  (:import (org.stringtemplate.v4 AttributeRenderer STGroupFile StringRenderer)))

(def string-renderer
  (reify AttributeRenderer
    (toString [_this value format _locale]
      (do
        (when (not (instance? String value))
          (throw (IllegalArgumentException. "expected a String")))
        (case format
          nil value
          "cap" (str (str/upper-case (subs value 0 1))
                     (subs value 1))
          "low" (str (str/lower-case (subs value 0 1))
                     (subs value 1)))))))

(def stgf (let [tmp (STGroupFile. (io/resource "templates/template.stg"))]
            (.registerRenderer tmp String string-renderer)
            #_(.registerRenderer tmp String (StringRenderer.))
            tmp))

(defn template [t]
  (or (.getInstanceOf stgf t)
      (throw (IllegalArgumentException. (str "No template named '" t "'")))))

(defn render [t m]
  (reduce
    (fn [result [k v]]
      (.add result (name k) (stringify-keys v)))
    (template t)
    m))

(defn directory [m] (render "directory" m))
(defn record [m] (render "record" m))
(defn wither [m] (render "wither" m))
(defn empty-fn [m] (render "emptyFactory" m))
