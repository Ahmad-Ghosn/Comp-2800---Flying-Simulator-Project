package Flight;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.behaviors.keyboard.KeyNavigator;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.LinkedList;

public class Flight_Control_Behaviour extends Behavior implements KeyListener {
	TransformGroup target;
	Transform3D transform;
	WakeupOnAWTEvent trigger;
	
	private WakeupCriterion k_down;
	private WakeupCriterion k_up;
	private WakeupOnElapsedFrames frame;
	private WakeupCondition wake;
	private Boolean listener;
	private Flight_Control keyNavigator_target;
	private Flight_Control keyNavigator_view;
	private KeyEvent k_event;
	private LinkedList eventq;

	private WakeupCriterion[] wakeList;
	public Flight_Control_Behaviour(TransformGroup target, TransformGroup view) {
		target.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.k_down = new WakeupOnAWTEvent(401);
		this.k_up = new WakeupOnAWTEvent(402);
		this.frame = new WakeupOnElapsedFrames(0);
		this.wakeList = new WakeupCriterion[]{this.k_down, this.k_up, this.frame};
		this.wake = new WakeupOr(this.wakeList);
		this.listener = false;
		this.keyNavigator_target = new Flight_Control(target);
		this.keyNavigator_view = new Flight_Control(view);
	}
	
	public void initialize() {
		if (this.listener){
			this.k_down = new WakeupOnBehaviorPost(this,401);
			this.k_up = new WakeupOnBehaviorPost(this, 402);
			this.wakeList[0] = k_down;
			this.wakeList[1] = k_up;
			this.wake = new WakeupOr(wakeList);
			this.eventq = new LinkedList();
		}
		this.wakeupOn(this.wake);
	}
	
	@Override
	public void processStimulus(Iterator<WakeupCriterion> criteria) {
		boolean sawFrame = false;
		
		while(true){
			while (criteria.hasNext()){
				WakeupCriterion event = criteria.next();
				if (event instanceof WakeupOnAWTEvent){
					WakeupOnAWTEvent event_AWT = (WakeupOnAWTEvent) event;
					AWTEvent[] events = event_AWT.getAWTEvent();
					this.processAWTEvent(events);
				} else if (event instanceof WakeupOnElapsedFrames && this.k_event != null) {
					sawFrame = true;
				} else if (event instanceof WakeupOnBehaviorPost) {
					while(true) {
						synchronized (this.eventq) {
							if(this.eventq.isEmpty()) {
								break;
							}
							this.k_event = (KeyEvent) this.eventq.remove(0);
							if(this.k_event.getID() == 401 || this.k_event.getID() == 402) {
								System.out.println("Keypress");
								this.keyNavigator_target.processKeyEvent(this.k_event);
								this.keyNavigator_view.processKeyEvent(this.k_event);
							}
						}
					}
				}
			}
			if(sawFrame){
				this.keyNavigator_target.processKeyEvent(this.k_event);
				this.keyNavigator_view.processKeyEvent(this.k_event);
			}
			this.wakeupOn(wake);
			return;
		}
	}
	
	private void processAWTEvent(AWTEvent[] events){
		for(int loop = 0; loop < events.length; ++loop) {
			if (events[loop] instanceof KeyEvent) {
				this.k_event = (KeyEvent)events[loop];
				if (this.k_event.getID() == 401 || this.k_event.getID() == 402) {
					this.keyNavigator_target.processKeyEvent(this.k_event);
					this.keyNavigator_view.processKeyEvent(this.k_event);
				}
			}
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		synchronized(this.eventq) {
			this.eventq.add(e);
			if (this.eventq.size() == 1) {
				this.postId(401);
			}
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		synchronized(this.eventq) {
			this.eventq.add(e);
			if (this.eventq.size() == 1) {
				this.postId(402);
			}
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) { }
}