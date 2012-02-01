package com.mattfeury.saucillator.android.instruments;

import java.util.LinkedList;

import com.mattfeury.saucillator.android.sound.Lagger;
import com.mattfeury.saucillator.android.sound.Limiter;

/**
 * A complex oscillator.
 * An oscillator that is made up of BasicOscs and sums them together.
 */
public class ComplexOsc extends Oscillator {

  protected LinkedList<Oscillator> components;

  public static final float MAX_AMPLITUDE = 1.0f;//what is this for, eh?

  private float maxInternalAmp = 1.0f, // internalAmp always ranges from 0-1
                internalAmp = 0f; // used to calculate attack and release to/from maxInternalAmp

  protected float attack = 0.2f,  // these are really just a percentage
                  release = 0.2f; // can we translate them to something more meaningful?
  protected Lagger attackLagger = new Lagger(0f, 1f),
                   releaseLagger = new Lagger(1f, 0f);
  protected boolean attacking = false, releasing = false, envelopeEnabled = true;

  public ComplexOsc() {
    this(1.0f);
  }
  public ComplexOsc(float amp) {
    amplitude = amp;
    components = new LinkedList<Oscillator>();
  }

  public void fill(Oscillator... oscs) {
    for(Oscillator osc : oscs) {
      osc.setPlaying(true); //we manage playback here, so all the children should always be playing
      components.add(osc);
      osc.chuck(this);
    }
  }
  public LinkedList<Oscillator> getComponents() {
    return components;
  }
  public Oscillator getComponent(int index) {
    return components.get(index);
  }

  public void setFreq(float freq) {
    for(Oscillator osc : components)
      osc.setFreq(freq * this.harmonic);
  }

  // TODO 
  // Let's make an Effect object, keep a list, and cycle through.
  public ComplexOsc resetEffects() {
    setModRate(0);
    setModDepth(0);
    setLag(0);

    envelopeEnabled = false;
    //setAttack(0);
    //setRelease(0);

    // For chaining. Because, why not?
    return this;
  }

  public void setModRate(int rate) {
    for(Oscillator osc : components)
      osc.setModRate(rate);
  }
  public void setModDepth(int depth) {
    for(Oscillator osc : components)
      osc.setModDepth(depth);
  }

  public int getModRate() {
    for(Oscillator osc : components)
      return osc.getModRate();

    return 0;
  }
  public int getModDepth() {
    for(Oscillator osc : components)
      return osc.getModDepth();

    return 0;
  }

  public float getLag() {
    for(Oscillator osc : components)
      return osc.getLag();
    
    return 0;
  }
public void setLag(float rate) {
    for(Oscillator osc : components)
      osc.setLag(rate);
  }
  public void setMaxInternalAmp(float amp) {
    this.maxInternalAmp = amp;
  }
  public float getMaxInternalAmp() {
    return maxInternalAmp;
  }

  /**
   * Envelope stuff
   * Maybe make this an interface or something
   */
  @Override
  public void togglePlayback() {
    if ((releasing && isPlaying()) || ! isPlaying())
      startAttack();
    else
      startRelease();
  }
  public boolean isReleasing() {
    return releasing;
  }
  public boolean isAttacking() {
    return attacking;
  }

  public float getAttack() {
    return attack;
  }
  public void setAttack(float a) {
    attack = a;
    resetLaggers();
  }
  public float getRelease() {
    return release;
  }
  public void setRelease(float r) {
    release = r;
    resetLaggers();
  }
  public void startAttack() {
    resetLaggers();

    this.start();
    releasing = false;
    attacking = true;
  }
  public void startRelease() {
    resetLaggers();

    attacking = false;
    releasing = true;
  }
  public void resetLaggers() {
    //TODO set rates
    attackLagger = new Lagger(internalAmp, maxInternalAmp);
    releaseLagger = new Lagger(internalAmp, 0f);

    //attackLagger.setRate(attack);
    //releaseLagger.setRate(release);
  }
  public void updateEnvelope() {
    float previousAmp = internalAmp;
    if (attacking) {
      internalAmp = attackLagger.update();
    } else if (releasing) {
      internalAmp = releaseLagger.update();
    }

    if (internalAmp == previousAmp && (attacking || releasing)) {
      attacking = false;

      if (releasing) {
        releasing = false;
        this.stop();
      }
    }
  }

  public void rendered() {
    if (envelopeEnabled)
      updateEnvelope();
  }  

  public synchronized boolean render(final float[] buffer) { // assume t is in 0.0 to 1.0
    if(! isPlaying()) {
      return true;
    }

    Limiter.limit(buffer);
    final float[] kidsBuffer = new float[CHUNK_SIZE];
    boolean didWork = renderKids(kidsBuffer);
    for(int i = 0; i < CHUNK_SIZE; i++) {
      if (envelopeEnabled)
        buffer[i] += amplitude*internalAmp*kidsBuffer[i];
      else
        buffer[i] += amplitude*kidsBuffer[i];
    }

    rendered();

    return didWork;
  }
}