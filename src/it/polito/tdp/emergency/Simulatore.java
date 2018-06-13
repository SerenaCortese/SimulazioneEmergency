package it.polito.tdp.emergency;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Simulatore {

	// Parametri
	private int NS = 3; // numero di studi medici
	private int NP = 50; // numero di pazienti in arrivo
	private int T_ARRIVAL = 15; // intervallo di tempo tra i pazienti (in minuti)

	private LocalTime T_inizio = LocalTime.of(8, 0);
	private LocalTime T_fine = LocalTime.of(20, 0);

	private int DURATION_TRIAGE = 5;
	private int DURATION_WHITE = 10;
	private int DURATION_YELLOW = 15;
	private int DURATION_RED = 30;
	private int TIMEOUT_WHITE = 120;
	private int TIMEOUT_YELLOW = 60;
	private int TIMEOUT_RED = 90;
	private int T_POLLING = 5; //definisco una costante per il priodo di controllo degli atudi vuoti=5minuti

	// Modello del mondo
	private List<Paziente> pazienti;//pazienti che son lì al momento(se muore o se ne va, andrà tolto)
	private StatoPaziente statoTriage; // il prossimo stato da assegnare
	private int studi_occupati;
	private PriorityQueue<Paziente> attesa;
	//non è lista di tutti i pazienti, ma coda prioritaria di pazienti che ha passato il triage ed è in attesa,ordinata come voglio io

	// Valori in output
	private int paz_curati;
	private int paz_abbandonati;
	private int paz_morti;

	// Coda degli eventi
	private PriorityQueue<Event> queue = new PriorityQueue<Event>();
	
	public void init() {
		this.pazienti = new ArrayList<>();
		this.attesa = new PriorityQueue<>(new PazienteComparator());
		//comparatore confronta pazienti in lista d'attesa=il primo ad esser chiamato è il primo nella lista
		
		LocalTime ora = T_inizio;

		for (int i = 0; i < NP; i++) {
			Paziente p = new Paziente(i + 1, StatoPaziente.NEW, ora);
			Event e = new Event(ora, EventType.ARRIVA, p);
			ora = ora.plusMinutes(T_ARRIVAL);
			//metto evento nella coda se no è inutile
			queue.add(e);
		}
		
		queue.add(new Event(T_inizio, EventType.POLLING, null));

		this.studi_occupati = 0; //al mattino nessuno studio è occupato
		
		statoTriage = StatoPaziente.WHITE;

		paz_curati = 0;
		paz_abbandonati = 0;
		paz_morti = 0;
	}

	public void run() {
		Event e;
		while ((e = queue.poll()) != null) {//itero finché ho eventi nella coda
			if (e.getOra().isAfter(T_fine)) //se sono oltre l'ora limite non elaboro quell'evento
				break;

			processEvent(e);
		}
	}

	private void processEvent(Event e) {
		switch(e.getTipo()) {
		case ARRIVA:
			Event e2 = new Event(e.getOra().plusMinutes(DURATION_TRIAGE),
					EventType.TRIAGE, e.getPaziente()) ;
			queue.add(e2) ; //rimetto paziente in coda con tempo dopo 5 minuti(=tempo triage)
			break ;

		case TRIAGE://hai passato il triage e sei in attesa
			e.getPaziente().setStato(statoTriage);
			
			//schedulo gli eventi generati da TIMEOUT(che si ha dopo che è stato assegnato codice)
			if(statoTriage==StatoPaziente.WHITE) {
				queue.add(new Event(e.getOra().plusMinutes(TIMEOUT_WHITE),
						EventType.TIMEOUT_WHITE, e.getPaziente())) ;
			} else if (statoTriage==StatoPaziente.YELLOW) {
				queue.add(new Event(e.getOra().plusMinutes(TIMEOUT_YELLOW),
						EventType.TIMEOUT_YELLOW, e.getPaziente())) ;
			} else if (statoTriage==StatoPaziente.RED) {
				queue.add(new Event(e.getOra().plusMinutes(TIMEOUT_RED),
						EventType.TIMEOUT_RED, e.getPaziente())) ;
			}
			
			attesa.add(e.getPaziente()); //metto paziente in lista d'attesa
			
			// cambiaStatoTriage:meccanismo per dare colori a rotazione non stato del paziente dopo attesa
			if(statoTriage==StatoPaziente.WHITE)//se ho dato a questo paziente un bianco, al prox darò giallo
				statoTriage=StatoPaziente.YELLOW ;
			else if(statoTriage==StatoPaziente.YELLOW)
				statoTriage=StatoPaziente.RED ;
			else if(statoTriage==StatoPaziente.RED)
				statoTriage=StatoPaziente.WHITE ;

			break;
			
		case CHIAMATA:
			//lo tolgo da attesa
			attesa.remove(e.getPaziente());
			
			//metto paziente in studio
			studi_occupati++;
			
			//schedulo uscita: aggiungo evento di quando uscirà
			//lo faccio prima del cambio stato per non perdere durata dello stato attuale
			switch(e.getPaziente().getStato()) {
			case WHITE :
				queue.add(new Event(e.getOra().plusMinutes(DURATION_WHITE), EventType.USCITA, e.getPaziente()));
				break;
			case YELLOW:
				queue.add(new Event(e.getOra().plusMinutes(DURATION_YELLOW), EventType.USCITA, e.getPaziente()));
				break;
			case RED:
				queue.add(new Event(e.getOra().plusMinutes(DURATION_RED), EventType.USCITA, e.getPaziente()));
				break;
			}
			
			//cambio lo stato in INCURA
			e.getPaziente().setStato(StatoPaziente.TREATING);

			break;
			
			
		case USCITA:
			// Registrare l'uscita di e.getPaziente(): paziente curato
			e.getPaziente().setStato(StatoPaziente.OUT);
			paz_curati++ ;
			studi_occupati-- ;
			
			// Studio libero => Decidere chi deve essere chiamato 
			//(sulla base di colore e ora di arrivo) tra i pazienti attesa(statoPaziente.WAIT con priorità più alta)
			Paziente paz = attesa.poll();//guardo primo elemento ma non lo tolgo dalla lista
			if(paz!= null) {
				//lo chiamo:Schedulo per ADESSO la CHIAMATA del paziente
				queue.add(new Event(e.getOra(), EventType.CHIAMATA, paz));
						//ora dell'evento quindi finisce in testa alla coda
			}
			//else: ho finito perché non c'è più nessuno da chiamare
			
			break;
			
		case TIMEOUT_WHITE:
			//vado a casa: metto lo stato in OUT e lo tolgo dalla coda in attesa
			//=>devo controllare che sia ancora in attesa(magari è stato già chiamato)
			if(e.getPaziente().getStato() == StatoPaziente.WHITE) {
				attesa.remove(e.getPaziente());
				paz_abbandonati++ ;
				e.getPaziente().setStato(StatoPaziente.OUT);
			}
			break;
		case TIMEOUT_YELLOW:
			//passa a rosso e devo impostare il timeout del rosso
			if(e.getPaziente().getStato() == StatoPaziente.YELLOW) {
				attesa.remove(e.getPaziente());//lo tolgo dalla coda perché se no rimane in quella posizione anche cambiando lo stato in rosso
				e.getPaziente().setStato(StatoPaziente.RED);
				queue.add(new Event(e.getOra().plusMinutes(DURATION_RED), EventType.USCITA, e.getPaziente()));
				attesa.add(e.getPaziente());//lo reinserisco in una posizione più privilegiata
			}
			
			break;
		case TIMEOUT_RED:
			if(e.getPaziente().getStato() == StatoPaziente.RED) {
				attesa.remove(e.getPaziente());
				paz_morti++ ;
				e.getPaziente().setStato(StatoPaziente.DEAD);//setto a morto il paziente
			}
			break;
		
		case POLLING:
			//vede se il numero di studi occupato è <num totale degli studi e lista d'attesa non è vuota
			if(studi_occupati<NS && !attesa.isEmpty()) {
				//chiamo primo nella lista: schedulo chiamata del primo in attesa per SUBITO
				Paziente paz2 = attesa.peek();
				queue.add(new Event(e.getOra(), EventType.CHIAMATA, paz2));
			}
			//lo schedulo perché controlli periodicamente (ogni 5 minuti= T_POLLING)
			queue.add(new Event(e.getOra().plusMinutes(T_POLLING), EventType.POLLING, null));
			
			break;
		}
		
	}

	public int getPaz_curati() {
		return paz_curati;
	}

	public int getPaz_abbandonati() {
		return paz_abbandonati;
	}

	public int getPaz_morti() {
		return paz_morti;
	}

	public void setNS(int nS) {
		NS = nS;
	}

	public void setNP(int nP) {
		NP = nP;
	}

	public void setT_ARRIVAL(int t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public void setT_inizio(LocalTime t_inizio) {
		T_inizio = t_inizio;
	}

	public void setT_fine(LocalTime t_fine) {
		T_fine = t_fine;
	}

	public void setDURATION_TRIAGE(int dURATION_TRIAGE) {
		DURATION_TRIAGE = dURATION_TRIAGE;
	}

	public void setDURATION_WHITE(int dURATION_WHITE) {
		DURATION_WHITE = dURATION_WHITE;
	}

	public void setDURATION_YELLOW(int dURATION_YELLOW) {
		DURATION_YELLOW = dURATION_YELLOW;
	}

	public void setDURATION_RED(int dURATION_RED) {
		DURATION_RED = dURATION_RED;
	}

	public void setTIMEOUT_WHITE(int tIMEOUT_WHITE) {
		TIMEOUT_WHITE = tIMEOUT_WHITE;
	}

	public void setTIMEOUT_YELLOW(int tIMEOUT_YELLOW) {
		TIMEOUT_YELLOW = tIMEOUT_YELLOW;
	}

	public void setTIMEOUT_RED(int tIMEOUT_RED) {
		TIMEOUT_RED = tIMEOUT_RED;
	}

	public void setT_POLLING(int t_POLLING) {
		T_POLLING = t_POLLING;
	}

}
