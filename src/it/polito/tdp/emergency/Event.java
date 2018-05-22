package it.polito.tdp.emergency;

import java.time.LocalTime;

public class Event implements Comparable<Event>{
	private LocalTime ora ; //LocalTime perché ho orari espliciti
	private EventType tipo ;
	
	private Paziente paziente ;//puntatore alla List in Simulatore

	public Event(LocalTime ora, EventType tipo, Paziente paziente) {
		super();
		this.ora = ora;
		this.tipo = tipo;
		this.paziente = paziente;
	}

	public LocalTime getOra() {
		return ora;
	}

	public EventType getTipo() {
		return tipo;
	}

	public Paziente getPaziente() {
		return paziente;
	}

	@Override
	public int compareTo(Event other) {//ordine crescente
		return this.ora.compareTo(other.getOra()) ;
	}
	
	

}
