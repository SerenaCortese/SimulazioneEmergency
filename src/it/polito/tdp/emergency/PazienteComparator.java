package it.polito.tdp.emergency;

import java.util.Comparator;

public class PazienteComparator implements Comparator<Paziente> {

	@Override
	public int compare(Paziente paz1, Paziente paz2) {
		//numero positivo se paz1 deve esser curato dop =>dopo nella coda
		//numero negativo se paz1 deve esser curato prima=>prima nella coda
		//zero se stessa priorità
		if(paz1.getStato() ==paz2.getStato()) {
			//stesso colore=>passa chi è arrivato prima
			return paz1.getOraArrivo().compareTo(paz2.getOraArrivo());
		}
		if(paz1.getStato()==StatoPaziente.RED)
			return -1; //tocca al paz1
		if(paz2.getStato()== StatoPaziente.RED)
			return +1; //secondo maggiore del primo
		if(paz1.getStato()==StatoPaziente.YELLOW)
			return -1; //tocca al paz1
		if(paz2.getStato()== StatoPaziente.YELLOW)
			return +1; //secondo maggiore del primo
		
		//se succede che tutti questi if falliscono, magari lo stato è out=> confronto pazienti non in lista d'attesa=>ERRORE
		throw new IllegalArgumentException("Stato paziente non accettabile: "+paz1.getStato()+ " - "+paz2.getStato());
	}

}
