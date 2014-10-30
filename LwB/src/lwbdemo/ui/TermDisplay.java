/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lwbdemo.ui;

import lwbdemo.model.Term;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import nl.utwente.cs.caes.tactile.control.TactilePane;
import nl.utwente.cs.caes.tactile.control.TactilePane.Anchor;

class TermDisplay extends StackPane{
    private final Term term;
    private final Label termLabel;
    private final Map<Bowtie, ChangeListener<Boolean>> inUseListenerByBowtie;
    private final ChangeListener<Anchor> anchorListener;
    private final ChangeListener<Bounds> boundsListener;
    
    private final Bowtie parentBowtie;
    private Bowtie anchoredBowtie;
    
    public TermDisplay(Term term, Bowtie parentBowtie) {
        this.term = term;
        this.parentBowtie = parentBowtie;
        this.inUseListenerByBowtie = new HashMap<>();
        
        termLabel = new Label();
        termLabel.textProperty().bind(term.stringProperty());
        termLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 20));
        
        getChildren().add(termLabel);
        
        anchorListener = (observable, oldVal, newVal) -> {
            if (newVal == null) {
                hostBowtie(null);
            }
        };
        
        boundsListener = (observable, oldVal, newVal) -> {
            setMinSize(newVal.getWidth() + 15, newVal.getHeight() + 15);
        };
    }
    
    public Term getTerm() {
        return term;
    }
    
    public void setActive(boolean active) {
        if (active) {
            setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            setMinWidth(40);
            
            TactilePane.setTracker(this, (TactilePane) parentBowtie.getParent());
            TactilePane.setOnAreaEntered(this, event -> {
                if (event.getOther() instanceof TypeBlade && event.getOther() != parentBowtie.typeBlade) {
                    onAreaEntered(((TypeBlade) event.getOther()).getBowtie());
                }
            });
            TactilePane.setOnAreaLeft(this, event -> {
                if (event.getOther() instanceof TypeBlade && event.getOther() != parentBowtie.typeBlade) {
                    onAreaLeft(((TypeBlade) event.getOther()).getBowtie());
                }
            });
            TactilePane.setOnInProximity(this, event -> {
                if (event.getOther() instanceof TypeBlade && event.getOther() != parentBowtie.typeBlade) {
                    onInProximityOrArea(((TypeBlade) event.getOther()).getBowtie());
                }
            });
            TactilePane.setOnInArea(this, event -> {
                if (event.getOther() instanceof TypeBlade && event.getOther() != parentBowtie.typeBlade) {
                    onInProximityOrArea(((TypeBlade) event.getOther()).getBowtie());
                }
            });
            TactilePane.setOnProximityLeft(this, event -> {
                if (event.getOther() instanceof TypeBlade && event.getOther() != parentBowtie.typeBlade) {
                    onProximityLeft(((TypeBlade) event.getOther()).getBowtie());
                }
            });
        } else {
            TactilePane.setTracker(this, null);
            TactilePane.setOnAreaEntered(this, null);
            TactilePane.setOnAreaLeft(this, null);
            TactilePane.setOnInProximity(this, null);
            TactilePane.setOnInArea(this, null);
              
            setBackground(null);
            setMinWidth(-1);
        }
    }
    
    private void onAreaEntered(Bowtie bowtie) {
        ChangeListener<Boolean> listener = (observable, oldVal, newVal) -> {
            if (!newVal) {
                onDropped(bowtie);
            }
        };
        TactilePane.inUseProperty(bowtie).addListener(listener);
        inUseListenerByBowtie.put(bowtie, listener);
    }
    
    private void onAreaLeft(Bowtie bowtie) {
        ChangeListener listener = inUseListenerByBowtie.remove(bowtie);
        if (listener != null) {
            TactilePane.inUseProperty(bowtie).removeListener(listener);
        }
    }
    
    private void onInProximityOrArea(Bowtie bowtie) {
        if (!term.canBeSet(bowtie.getType())) {
            Border red = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2)));
            bowtie.typeBlade.setBorder(red);
            this.setBorder(red);
        } else {
            Border green = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2)));
            bowtie.typeBlade.setBorder(green);
            this.setBorder(green);
        }
    }
    
    private void onProximityLeft(Bowtie bowtie) {
        bowtie.typeBlade.setBorder(null);
        this.setBorder(null);
    }
    
    private void onDropped(Bowtie bowtie) {
        hostBowtie(bowtie);
        ChangeListener<Boolean> inUseListener = inUseListenerByBowtie.get(bowtie);
        if (inUseListener != null) {
            TactilePane.inUseProperty(bowtie).removeListener(inUseListener);
        }
    }
    
    public void hostBowtie(Bowtie bowtie) {
        if (bowtie == anchoredBowtie) {
            return;
        }
        
        if (bowtie == null) {
            term.setTerm(null);
            
            anchoredBowtie.anchorAt(null);
            TactilePane.anchorProperty(anchoredBowtie).removeListener(anchorListener);
            
            anchoredBowtie.termBlade.boundsInParentProperty().removeListener(boundsListener);
            setMinSize(-1, -1);
            
            anchoredBowtie = null;
            
            getChildren().add(termLabel);
            setActive(true);
        } else if (TactilePane.getAnchor(bowtie) == null && term.setTerm(bowtie.getType())) {
            setActive(false);
            getChildren().remove(termLabel);
            
            bowtie.anchorAt(this);
            TactilePane.anchorProperty(bowtie).addListener(anchorListener);
            anchoredBowtie = bowtie;
            
            Bounds anchoredBounds = anchoredBowtie.termBlade.getBoundsInParent();
            setMinSize(anchoredBounds.getWidth() + 15, anchoredBounds.getHeight() + 15);
            anchoredBowtie.termBlade.boundsInParentProperty().addListener(boundsListener);
        } else if (TactilePane.getAnchor(bowtie) == null) {
            TactilePane.moveAwayFrom(bowtie.typeBlade, this, 500);
            TactilePane.moveAwayFrom(this, bowtie.typeBlade, 500);
        }
    }
}
