package com.litebrowser.helper.interfaces

interface LabelListChangedInterface {
    fun onEndList()
    fun onStartList(labelList: ArrayList<String>)
}