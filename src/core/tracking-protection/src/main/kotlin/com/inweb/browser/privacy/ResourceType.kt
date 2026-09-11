package com.inweb.browser.privacy

/** Resource types a network request can have (EasyList option tokens). */
enum class ResourceType {
    DOCUMENT,
    SCRIPT,
    IMAGE,
    STYLESHEET,
    XHR,
    SUBDOCUMENT,
    OBJECT,
    WEBSOCKET,
    POPUP,
    MEDIA,
    FONT,
    OTHER,
}
