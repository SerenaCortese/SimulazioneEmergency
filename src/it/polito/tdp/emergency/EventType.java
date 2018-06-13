package it.polito.tdp.emergency;

public enum EventType {
	ARRIVA, // arriva nuovo paziente all'ingresso(new stato)
	TRIAGE, // al paziente viene assegnato un codice(new-->colore)
	CHIAMATA, // il paziente entra dal medico(colore-->treating)
	USCITA, // il paziente esce dallo studio medico(treating-->out)
	
	TIMEOUT_WHITE,
	TIMEOUT_YELLOW,
	TIMEOUT_RED,
	
	POLLING,//periodicamente verifica se ci sono studi liberi e pazienti in attesa
}
