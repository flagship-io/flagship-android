package com.abtasty.flagship.eai

interface OnEAIEvents {
    fun onEAIClickEvent(click: String)
    fun onEAIScrollEvent(scroll: String, moves: String?)
    fun onEAIMoveEvent(moves: String)
}