package com.droiddeck.launcher.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class PcLayoutElement(val id:String,var type:String,var x:Float,var y:Float,var size:Float,var label:String,var key:String="",var up:String="",var down:String="",var left:String="",var right:String="")

class PcLayoutStore(private val c:Context){
 private val p=c.getSharedPreferences("pc_layouts",0)
 fun load(profile:String):MutableList<PcLayoutElement>{
  val raw=p.getString(profile,null) ?: return defaults().toMutableList()
  return runCatching{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);PcLayoutElement(o.getString("id"),o.getString("type"),o.getDouble("x").toFloat(),o.getDouble("y").toFloat(),o.getDouble("size").toFloat(),o.optString("label"),o.optString("key"),o.optString("up"),o.optString("down"),o.optString("left"),o.optString("right"))}.toMutableList()}.getOrElse{defaults().toMutableList()}
 }
 fun save(profile:String,e:List<PcLayoutElement>){val a=JSONArray();e.forEach{v->a.put(JSONObject().apply{put("id",v.id);put("type",v.type);put("x",v.x);put("y",v.y);put("size",v.size);put("label",v.label);put("key",v.key);put("up",v.up);put("down",v.down);put("left",v.left);put("right",v.right)})};p.edit().putString(profile,a.toString()).apply()}
 fun reset(profile:String)=p.edit().remove(profile).apply()
 private fun defaults()=listOf(
  PcLayoutElement("left_stick","stick",.16f,.75f,.15f,"WASD",up="W",down="S",left="A",right="D"),
  PcLayoutElement("right_stick","mouse",.84f,.75f,.15f,"MOUSE"),
  PcLayoutElement("lmb","mouse_button",.64f,.28f,.075f,"LMB",key="MOUSE_LEFT"),
  PcLayoutElement("rmb","mouse_button",.78f,.28f,.075f,"RMB",key="MOUSE_RIGHT"),
  PcLayoutElement("space","button",.48f,.88f,.065f,"SPACE",key="SPACE"),
  PcLayoutElement("e","button",.90f,.58f,.055f,"E",key="E"),
  PcLayoutElement("r","button",.90f,.46f,.055f,"R",key="R"),
  PcLayoutElement("shift","button",.06f,.54f,.055f,"SHIFT",key="SHIFT"),
  PcLayoutElement("ctrl","button",.06f,.66f,.055f,"CTRL",key="CTRL"),
  PcLayoutElement("touch","touch",.50f,.48f,.28f,"TOUCH"))
}

class PcLayoutView(c:Context,private val profile:String,private val editing:Boolean=false,private val onKey:(Int,Boolean)->Unit={_,_->},private val pointer:PointerGestures.Listener?=null,private val onDirectTouch:((MotionEvent)->Unit)?=null):View(c){
 private val store=PcLayoutStore(c);val elements=store.load(profile);var directTouch=true;var selectedId:String?=null
 private val paint=Paint(1);private val active=HashMap<Int,PcLayoutElement>()
 init{isClickable=true}
 fun save()=store.save(profile,elements)
 fun reset(){store.reset(profile);elements.clear();elements.addAll(store.load(profile));invalidate()}
 fun addButton(){elements.add(PcLayoutElement("button_${System.currentTimeMillis()}","button",.5f,.5f,.055f,"KEY",key="E"));invalidate()}
 fun selected()=elements.firstOrNull{it.id==selectedId}
 fun growSelected(d:Float){selected()?.let{it.size=(it.size+d).coerceIn(.03f,.25f)};invalidate()}
 override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();for(e in elements){val cx=e.x*w;val cy=e.y*h;val r=e.size*min(w,h);val q=RectF(cx-r,cy-r,cx+r,cy+r);paint.style=Paint.Style.FILL;paint.color=if(editing&&selectedId==e.id)0x6655AAFF else 0x66303038;c.drawRoundRect(q,r,r,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=if(editing&&selectedId==e.id)4f else 2f;paint.color=if(editing&&selectedId==e.id)0xFF55AAFF.toInt() else 0xFFB8BDC7.toInt();c.drawRoundRect(q,r,r,paint);paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.CENTER;paint.textSize=max(16f,r*.32f);paint.color=0xFFF4F6FA.toInt();c.drawText(e.label,cx,cy-(paint.ascent()+paint.descent())/2f,paint)}}
 override fun onTouchEvent(ev:MotionEvent):Boolean{val x=ev.x/max(1f,width);val y=ev.y/max(1f,height);when(ev.actionMasked){MotionEvent.ACTION_DOWN->{val e=hit(x,y);if(e!=null){selectedId=e.id;active[ev.getPointerId(0)]=e;if(!editing)press(e,ev.x,ev.y);invalidate();return true};if(!editing&&directTouch){onDirectTouch?.invoke(ev);return true};return editing};MotionEvent.ACTION_MOVE->{for(i in 0 until ev.pointerCount){val e=active[ev.getPointerId(i)]?:continue;if(editing){e.x=(ev.getX(i)/max(1f,width)).coerceIn(.02f,.98f);e.y=(ev.getY(i)/max(1f,height)).coerceIn(.02f,.98f)}else move(e,ev.getX(i),ev.getY(i))};if(!editing&&directTouch&&active.isEmpty())onDirectTouch?.invoke(ev);invalidate();return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{val i=ev.actionIndex;active.remove(ev.getPointerId(i))?.let{if(!editing)release(it,ev.getX(i),ev.getY(i))};if(!editing&&directTouch&&active.isEmpty())onDirectTouch?.invoke(ev);invalidate();return true}};return true}
 private fun hit(x:Float,y:Float)=elements.asReversed().firstOrNull{val dx=x-it.x;val dy=y-it.y;dx*dx+dy*dy<=(it.size*1.35f)*(it.size*1.35f)}
 private fun press(e:PcLayoutElement,x:Float,y:Float){when(e.type){"button"->key(e.key,true);"mouse_button"->mouse(e.key,true,x,y);"stick"->stick(e,x,y)}}
 private fun move(e:PcLayoutElement,x:Float,y:Float){when(e.type){"stick"->stick(e,x,y);"mouse"->{val s=max(1f,min(width,height));pointer?.onMove((x-e.x*width)/s*22f,(y-e.y*height)/s*22f)};"touch"->onDirectTouch?.invoke(MotionEvent.obtain(System.currentTimeMillis(),System.currentTimeMillis(),MotionEvent.ACTION_MOVE,x,y,0))}}
 private fun stick(e:PcLayoutElement,x:Float,y:Float){val s=max(1f,e.size*min(width,height));val nx=((x-e.x*width)/s).coerceIn(-1f,1f);val ny=((y-e.y*height)/s).coerceIn(-1f,1f);key(e.up,ny<-.35f);key(e.down,ny>.35f);key(e.left,nx<-.35f);key(e.right,nx>.35f)}
 private fun key(n:String,d:Boolean){when(n.uppercase()){"MOUSE_LEFT"->return;"MOUSE_RIGHT"->return};keyCode(n)?.let{onKey(it,d)}}
 private fun mouse(n:String,d:Boolean,x:Float,y:Float){val b=when(n){"MOUSE_LEFT"->PointerGestures.BTN_LEFT;"MOUSE_RIGHT"->PointerGestures.BTN_RIGHT;"MOUSE_MIDDLE"->PointerGestures.BTN_MIDDLE;else->return};pointer?.onButton(b,d,x,y)}
 private fun release(e:PcLayoutElement,x:Float,y:Float){when(e.type){"button"->key(e.key,false);"mouse_button"->mouse(e.key,false,x,y);"stick"->{key(e.up,false);key(e.down,false);key(e.left,false);key(e.right,false)}}}
 private fun keyCode(n:String)=when(n.uppercase()){"W"->KeyEvent.KEYCODE_W;"A"->KeyEvent.KEYCODE_A;"S"->KeyEvent.KEYCODE_S;"D"->KeyEvent.KEYCODE_D;"E"->KeyEvent.KEYCODE_E;"R"->KeyEvent.KEYCODE_R;"F"->KeyEvent.KEYCODE_F;"Q"->KeyEvent.KEYCODE_Q;"SPACE"->KeyEvent.KEYCODE_SPACE;"SHIFT"->KeyEvent.KEYCODE_SHIFT_LEFT;"CTRL"->KeyEvent.KEYCODE_CTRL_LEFT;"ALT"->KeyEvent.KEYCODE_ALT_LEFT;"TAB"->KeyEvent.KEYCODE_TAB;"ESC"->KeyEvent.KEYCODE_ESCAPE;"ENTER"->KeyEvent.KEYCODE_ENTER;"BACKSPACE"->KeyEvent.KEYCODE_DEL;"UP"->KeyEvent.KEYCODE_DPAD_UP;"DOWN"->KeyEvent.KEYCODE_DPAD_DOWN;"LEFT"->KeyEvent.KEYCODE_DPAD_LEFT;"RIGHT"->KeyEvent.KEYCODE_DPAD_RIGHT;else->null}
}
