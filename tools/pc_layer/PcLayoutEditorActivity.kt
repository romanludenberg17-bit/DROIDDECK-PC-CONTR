package com.droiddeck.launcher.input

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

class PcLayoutEditorActivity:Activity(){
 private lateinit var view:PcLayoutView
 override fun onCreate(state:Bundle?){super.onCreate(state);val profile=intent.getStringExtra("profile")?:"default";val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setBackgroundColor(0xFF0B0F14.toInt());view=PcLayoutView(this,profile,true);root.addView(view,LinearLayout.LayoutParams(-1,0,1f));val bar=LinearLayout(this);bar.gravity=Gravity.CENTER
 fun b(t:String,a:()->Unit)=Button(this).apply{text=t;setOnClickListener{a()}}
 bar.addView(b("add"){view.addButton()});bar.addView(b("size-"){view.growSelected(-.01f)});bar.addView(b("size+"){view.growSelected(.01f)});bar.addView(b("map"){map()});bar.addView(b("reset"){view.reset()});bar.addView(b("save"){view.save();finish()});root.addView(bar,LinearLayout.LayoutParams(-1,-2));setContentView(root)}
 private fun map(){val e=view.selected()?:return;val choices=arrayOf("W","A","S","D","E","R","F","Q","SPACE","SHIFT","CTRL","ALT","TAB","ENTER","MOUSE_LEFT","MOUSE_RIGHT","MOUSE_MIDDLE");android.app.AlertDialog.Builder(this).setTitle("mapping: ${e.label}").setItems(choices){_,i->if(e.type=="stick")e.up=choices[i] else e.key=choices[i];view.invalidate()}.show()}
}
