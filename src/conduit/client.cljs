(ns conduit.client
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.tx-processing :as ftx]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [cljs.core.async.interop :refer-macros [<p!]]
    [com.fulcrologic.fulcro.application :as app]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [edn-query-language.core :as eql]
    [clojure.core.async :as async]))

;; TODO: Create a lib for "pathom remote"
(defn transmit!
  [{:keys [parser]
    :as   env} {::ftx/keys [id idx ast options update-handler
                            result-handler active?]}]
  (let [query (eql/ast->query ast)
        result (parser env query)]
    (async/go
      (result-handler {:body                 (async/<! result)
                       :original-transaction ast
                       :status-code          200}))))



(defsc TagPill [this {:conduit.tag/keys [tag]}]
  {:query [:conduit.tag/tag]
   :ident :conduit.tag/tag}
  (dom/li
    {:className "tag-default tag-pill tag-outline"
     :href      ""}
    tag))

(def ui-tag-pill (comp/factory TagPill {:keyfn :conduit.tag/tag}))

(defsc TagLink [this {:conduit.tag/keys [tag]}]
  {:query [:conduit.tag/tag]
   :ident :conduit.tag/tag}
  (dom/a
    {:className "tag-pill tag-default"
     :href      ""}
    tag))

(def ui-tag-link (comp/factory TagLink {:keyfn :conduit.tag/tag}))

(defsc PopularTags [this {::keys [popular-tags]}]
  {:ident         (fn [] [:component/id ::popular-tags])
   :query         [:component/id
                   ::popular-tags]
   :initial-state (fn [_]
                    {:component/id ::popular-tags})}
  (dom/div
    {:className "sidebar"}
    (dom/p "Popular Tags")
    (dom/div
      {:className "tag-list"}
      (map ui-tag-link popular-tags))))

(def ui-popular-tags (comp/factory PopularTags))

(defn ui-banner
  []
  (dom/div
    {:className "banner"}
    (dom/div
      {:className "container"}
      (dom/h1
        {:className "logo-font"}
        "conduit")
      (dom/p "A place to share your knowledge."))))

(defsc FeedToggle [this props]
  {:query []}
  (dom/div
    {:className "feed-toggle"}
    (dom/ul
      {:className "nav nav-pills outline-active"}
      (for [{::keys [label href]} [{::label "Your Feed"
                                    ::href  ""}
                                   {::label "Global Feed"
                                    ::href  ""}]]

        (dom/li
          {:key       label
           :className "nav-item"}
          (dom/a {:className "nav-link disabled"
                  :href      href}
                 label))))))

(def ui-feed-toggle (comp/factory FeedToggle))

(defsc ArticlePreview [this {:conduit.profile/keys [image username]
                             :conduit.article/keys [title created-at
                                                    description tag-list favorites-count]}]
  {:query [:conduit.article/description
           :conduit.article/title
           :conduit.article/tag-list
           :conduit.article/favorites-count
           :conduit.article/created-at
           :conduit.profile/image
           :conduit.profile/username
           :conduit.article/slug]
   :ident :conduit.article/slug}
  (dom/div
    {:className "article-preview"}
    (dom/div
      {:className "article-meta"}
      (dom/a
        {:href "profile.html"})
      (dom/img
        {:src image})
      (dom/div
        {:className "info"}
        (dom/a
          {:className "author"
           :href      ""}
          username)
        (dom/span
          {:className "date"}
          created-at))
      (dom/button
        {:className "btn btn-outline-primary btn-sm pull-xs-right"}
        (dom/i
          {:className "ion-heart"})
        favorites-count))
    (dom/a
      {:className "preview-link"
       :href      ""}
      (dom/h1 title)
      (dom/p description)
      (dom/span "Read more...")
      (dom/ul
        {:className "tag-list"}
        (map ui-tag-pill tag-list)))))

(def ui-article-preview (comp/factory ArticlePreview {:keyfn :conduit.article/slug}))

(defsc Feed [this {::keys  [articles]
                   :>/keys [popular-tags feed-toggle]
                   :as     props}]
  {:ident         (fn [] [:component/id ::feed])
   :query         [:component/id
                   {::articles (comp/get-query ArticlePreview)}
                   {:>/popular-tags (comp/get-query PopularTags)}
                   {:>/feed-toggle (comp/get-query FeedToggle)}]
   :initial-state (fn [_]
                    {:component/id   ::feed
                     :>/popular-tags (comp/get-initial-state PopularTags _)
                     :>/feed-toggle  (comp/get-initial-state FeedToggle _)})
   :will-enter    (fn [app _]
                    (dr/route-deferred [:component/id ::feed]
                                       #(df/load! app [:component/id ::feed] Feed
                                                  {:post-mutation        `dr/target-ready
                                                   :post-mutation-params {:target [:component/id ::feed]}})))
   :route-segment ["feed"]}
  (dom/div
    {:className "home-page"}
    (ui-banner)
    (dom/div
      {:className "container page"}
      (dom/div
        {:className "row"}
        (dom/div
          {:className "col-md-9"}
          (ui-feed-toggle feed-toggle)
          (map ui-article-preview articles))
        (dom/div
          {:className "col-md-3"}
          (ui-popular-tags popular-tags))))))

(defsc SignUp [this props]
  {:ident         (fn [] [:component/id ::sign-up])
   :query         []
   :route-segment ["sign-up"]}
  (dom/div
    {:className "auth-page"}
    (dom/div
      {:className "container page"}
      (dom/div
        {:className "row"}
        (dom/div
          {:className "col-md-6 offset-md-3 col-xs-12"}
          (dom/h1
            {:className "text-xs-center"}
            "Sign up")
          (dom/p
            {:className "text-xs-center"}
            (dom/a
              {:href ""}
              "Have an account?"))
          (dom/ul
            {:className "error-messages"}
            (dom/li "That email is already taken"))
          (dom/form
            (dom/fieldset
              {:className "form-group"}
              (dom/input
                {:className   "form-control form-control-lg"
                 :type        "text",
                 :placeholder "Your Name"}))
            (dom/fieldset
              {:className "form-group"}
              (dom/input
                {:className   "form-control form-control-lg"
                 :type        "text"
                 :placeholder "Email"}))
            (dom/fieldset
              {:className "form-group"}
              (dom/input
                {:className   "form-control form-control-lg"
                 :type        "password",
                 :placeholder "Password"}))
            (dom/button
              {:className "btn btn-lg btn-primary pull-xs-right"}
              "Sign up")))))))

(defsc SignIn [this props]
  {:ident         (fn [] [:component/id ::sign-in])
   :query         []
   :route-segment ["sign-in"]}
  (dom/div
    {:className "auth-page"}
    (dom/div
      {:className "container page"}
      (dom/div
        {:className "row"}
        (dom/div
          {:className "col-md-6 offset-md-3 col-xs-12"}
          (dom/h1
            {:className "text-xs-center"}
            "Sign in")
          (dom/p
            {:className "text-xs-center"}
            (dom/a
              {:href ""}
              "Have an account?"))
          (dom/ul
            {:className "error-messages"}
            (dom/li "That email is already taken"))
          (dom/form
            (dom/fieldset
              {:className "form-group"}
              (dom/input
                {:className   "form-control form-control-lg"
                 :type        "text"
                 :placeholder "Email"}))
            (dom/fieldset
              {:className "form-group"}
              (dom/input
                {:className   "form-control form-control-lg"
                 :type        "password",
                 :placeholder "Password"}))
            (dom/button
              {:className "btn btn-lg btn-primary pull-xs-right"}
              "Sign in")))))))

(defrouter TopRouter [this {:keys [current-state]}]
  {:router-targets [Feed SignIn SignUp]}
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-top-router (comp/factory TopRouter))

(defsc Header [this {:keys [com.fulcrologic.fulcro.routing.dynamic-routing/current-route]}]
  {:query         [:com.fulcrologic.fulcro.routing.dynamic-routing/current-route]
   :ident         (fn []
                    [:com.fulcrologic.fulcro.routing.dynamic-routing/id
                     :conduit.client/TopRouter])
   :initial-state (fn [_]
                    (comp/get-initial-state TopRouter _))}
  (dom/nav
    {:className "navbar navbar-light"}
    (dom/div
      {:className "container"}
      (dom/a {:className "navbar-brand"
              :href      "index.html"}
             "conduit")
      (dom/ul
        {:className "nav navbar-nav pull-xs-right"}
        (for [{::keys [label href]} [{::label "Home"
                                      ::href  "feed"}
                                     {::label "Sign Up"
                                      ::href  "sign-up"}
                                     {::label "Sign In"
                                      ::href  "sign-in"}]]
          (dom/li
            {:key       href
             :className "nav-item"}
            (dom/a
              {:onClick #(dr/change-route this [href])
               :classes ["nav-link"
                         (when  (some-> current-route
                                        second
                                        name
                                        (= href))
                           "active")]}
              label)
            ;;TODO: Back to href
            #_(dom/a
                {:href      href
                 :className "nav-link active"}
                label)))))))

(def ui-header (comp/factory Header))

(defsc Footer [this {::keys []}]
  {:query []}
  (dom/footer
    (dom/div
      {:className "container"}
      (dom/a {:className "logo-font"
              :href      "/"}
             "conduit")
      (dom/span
        {:className "attribution"}
        "An interactive learning project from "
        (dom/a {:href "https://thinkster.io"} "Thinkster")
        ". Code & design licensed under MIT."))))

(def ui-footer (comp/factory Footer))

(defsc Root [this {:>/keys [footer header router]
                   :as     props}]
  {:query         [{:>/header (comp/get-query Header)}
                   {:>/router (comp/get-query TopRouter)}
                   {:>/footer (comp/get-query Footer)}]
   :initial-state (fn [_]
                    {:>/header (comp/get-initial-state Header _)
                     :>/router (comp/get-initial-state TopRouter _)
                     :>/footer (comp/get-initial-state Footer _)})}
  (comp/fragment
    #_(debug props)
    (ui-header header)
    (ui-top-router router)
    (ui-footer footer)))

(defn client-did-mount
  "Must be used as :client-did-mount parameter of app creation, or called just after you mount the app."
  [app]
  (dr/change-route app ["feed"]))


(defn fetch
  [{::keys [api-url]} {::keys [path]}]
  (async/go
    (-> (js/fetch (str api-url path))
        <p!
        .json
        <p!)))

(def register
  [(pc/resolver `popular-tags
                {::pc/output [::popular-tags]}
                (fn [ctx _]
                  (async/go
                    (let [result (async/<! (fetch ctx {::path "/tags"}))
                          {:strs [tags]} (js->clj result)]
                      {::popular-tags (for [tag tags]
                                        {:conduit.tag/tag tag})}))))
   (pc/resolver `articles
                {::pc/output [:conduit.client/articles]}
                (fn [ctx _]
                  (async/go
                    (let [result (async/<! (fetch ctx {::path "/articles"}))
                          {:strs [articlesCount
                                  articles]} (js->clj result)]
                      {::articles-count articlesCount
                       ::articles       (for [{:strs [updatedAt body createdAt author favorited slug tagList favoritesCount title
                                                      description]} articles
                                              :let [{:strs [bio
                                                            following
                                                            image
                                                            username]} author
                                                    profile #:conduit.profile{:bio       bio
                                                                              :following following
                                                                              :image     image
                                                                              :username  username}]]
                                          (merge
                                            profile
                                            #:conduit.article{:updated-at      updatedAt
                                                              :body            body
                                                              :created-at      createdAt
                                                              :author          profile
                                                              :favorited?      favorited
                                                              :slug            slug
                                                              :tag-list        (for [tag tagList]
                                                                                 {:conduit.tag/tag tag})
                                                              :favorites-count favoritesCount
                                                              :title           title
                                                              :description     description}))}))))])

(def parser
  (p/parallel-parser
    {::p/plugins [(pc/connect-plugin {::pc/register register})]}))

(def remote
  {:transmit!               transmit!
   :parser                  parser
   ::api-url                "https://conduit.productionready.io/api"
   ::p/reader               [p/map-reader
                             pc/parallel-reader
                             pc/open-ident-reader
                             p/env-placeholder-reader]
   ::p/placeholder-prefixes #{">"}})

(defonce app (app/fulcro-app {:client-did-mount client-did-mount
                              :remotes          {:remote remote}}))

(def node "conduit")

(defn ^:export init-fn
  []
  (app/mount! app Root node))

