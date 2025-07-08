package br.jus.cnj.datajud.elasticToDatajud.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoIndicador;
import br.jus.cnj.datajud.elasticToDatajud.model.SituacaoJson;
import br.jus.cnj.datajud.elasticToDatajud.model.Assunto;
import br.jus.cnj.datajud.elasticToDatajud.model.Classe;
import br.jus.cnj.datajud.elasticToDatajud.model.Situacao;
import br.jus.cnj.datajud.elasticToDatajud.model.OrgaoJulgador;

@Component
public class DataCache {

	@Value("${postgresqlUrl}")
    private String url;

    @Value("${postgresqlUser}")
    private String user;

    @Value("${postgresqlPwd}")
    private String pwd;
    
    private Map<Integer, Classe> classes = new HashMap<Integer, Classe>(0);
	private Map<Integer, Assunto> assuntos = new HashMap<Integer, Assunto>(0);
	private List<Integer> movimentos = new ArrayList<Integer>(0);
	private List<String> prioridades = new ArrayList<String>(0);
	private Map<Integer, OrgaoJulgador> orgaosJulgadores = new HashMap<Integer, OrgaoJulgador>(0);
	private Map<String, Integer> graus = new HashMap<String, Integer>(0);
	private List<String> magistrados = new ArrayList<String>(0);
	private Map<String, Integer> tribunais = new HashMap<String, Integer>(0);
	private Map<Date, Integer> tempos = new HashMap<Date, Integer>(0);
	private List<Integer> niveisSigilo = new ArrayList<Integer>(0);
	private List<Integer> formatosProcesso = new ArrayList<Integer>(0);
	private List<Integer> sistemas = new ArrayList<Integer>(0);
	private Map<Integer, Situacao> situacoes = new HashMap<Integer, Situacao>(0);
	private Map<Pair<Integer,String>, Integer> situacoesMovimentos = new HashMap<Pair<Integer,String>, Integer>(0);
	Logger log = LoggerFactory.getLogger(DataCache.class);
	
	//Mapeamento de Situação em tipos de indicadores
	private final Integer TIPO_NAO_INFORMADO = 0;
    private final Integer TIPO_PENDENTE = 1;
    private final Integer[] situacoesPendente = new Integer[]{88};
    private final Integer TIPO_JULGAMENTO = 2;
    private final Integer[] situacoesJulgamento = new Integer[]{18, 27, 28, 29, 62, 72, 90, 129};
    private final Integer TIPO_SUSPENSAO = 3;
    private final Integer[] situacoesSuspensao = new Integer[]{45,  46, 47, 48, 49, 92, 93, 94, 95, 96, 128, 144};
    public final Integer TIPO_BAIXA = 4;
    private final Integer[] situacoesBaixa = new Integer[]{10, 23, 41};
    public final Integer TIPO_ARQUIVAMENTO = 5;
    private final Integer[] situacoesArquivamento = new Integer[]{2};
    private final Integer TIPO_REATIVACAO = 6;
    private final Integer[] situacoesIniciarReativacao = new Integer[]{37};
    private final Integer TIPO_DESSOBRESTAMENTO = 7;
    private final Integer[] situacoesDessobrestamento = new Integer[]{20, 82, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107};
    private final Integer TIPO_NOVO = 8;
    private final Integer[] situacoesIniciarNovo = new Integer[]{0, 9, 24, 26, 61, 65, 81, 91};
    private final Integer TIPO_ARQUIVAMENTO_PROVISORIO = 9;
    private final Integer[] situacoesArquivamentoProvisorio = new Integer[]{4};
    private final Integer TIPO_AUDIENCIA_REALIZADA = 10;
    private final Integer[] situacoesAudienciaRealizada = new Integer[]{6, 8, 44};
    private final Integer TIPO_CONCLUSO = 11;
    private final Integer[] situacoesConcluso = new Integer[]{12, 66, 67, 68, 69};
    private final Integer TIPO_DECISAO = 12;
    private final Integer[] situacoesDecisao = new Integer[]{3, 9, 14, 15, 16, 17, 19, 30, 31, 32, 34, 138, 139, 141, 142, 143};
    private final Integer TIPO_LIMINAR = 13;
    private final Integer[] situacoesLiminar = new Integer[]{33, 89};
    private final Integer TIPO_DESPACHO = 14;
    private final Integer[] situacoesDespacho = new Integer[]{21};
    private final Integer TIPO_PENDENTE_LIQUIDO = 15;
    private final Integer[] situacoesPendenteLiquido = new Integer[]{25};
    private final Integer TIPO_AUDIENCIA_REDESIGNADA = 16;
    private final Integer[] situacoesAudienciaRedesignada = new Integer[]{5, 7, 43, 73, 76, 77, 80, 83, 87};
    private final Integer TIPO_PROCEDIMENTO_RESOLVIDO = 17;
    private final Integer[] situacoesProcedimentoResolvido = new Integer[]{140};
    private final Integer TIPO_REDISTRIBUIDO = 18;
    private final Integer[] situacoesIniciarRedistribuido = new Integer[]{40, 118, 119, 120, 130, 131, 134, 153, 154};
    private final Integer TIPO_RECURSO_INTERNO = 19;
    private final Integer[] situacoesRecursoInterno = new Integer[]{39};
		
    @PostConstruct
	public void init() {
		log.info("Iniciando cache Datajud...");
		loadClasses();
		loadAssuntos();
		loadMovimentos();
		loadPrioridades();
		loadOrgaosJulgadores();
		loadGraus();
		loadTribunais();
		loadNiveisSigilo();
		loadFormatosProcesso();
		loadSistemas();
		loadTempos();
		loadSituacoes();
		loadSituacoesMovimentos();
		log.info("Cache Datajud iniciado!");
	}
	
	private Connection getConnection() {
        Connection con = null;
        try {  
			Class.forName("org.postgresql.Driver");  
			con = DriverManager.getConnection(url,user,pwd);
        }
        catch(Exception e) { 
        	throw new RuntimeException("Erro ao conectar ao banco da dados Postgres: " + e.getLocalizedMessage());
    	}
		return con;
	}
        
	private void loadMovimentos() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id from movimento");
			while(rs.next()) {
				this.movimentos.add(rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de movimentos: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadPrioridades() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id from tipo_prioridade");
			while(rs.next()) {
				this.prioridades.add(rs.getString("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de prioridades: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadOrgaosJulgadores() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select id, id_tribunal, seq_classificacao_unid_judiciaria from orgao_julgador");
			while (rs.next()) {
				this.orgaosJulgadores.put(rs.getInt("id"), new OrgaoJulgador(rs.getInt("id"), rs.getInt("id_tribunal"), rs.getInt("seq_classificacao_unid_judiciaria")));
			}
		} catch (Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de Órgãos Julgadores: %s", e.getLocalizedMessage()));
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadClasses() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select id, id_fase_processual, id_tipo_procedimento, id_natureza_procedimento, criminal from classe");
			while(rs.next()) {
				Integer id = rs.getInt("id");
				Integer idFase = rs.getInt("id_fase_processual");
				Integer idTipoProcedimento = rs.getInt("id_tipo_procedimento");
				Integer idNatureza = rs.getInt("id_natureza_procedimento");
				Boolean criminal = rs.getBoolean("criminal");
				
				Classe c = new Classe();
				c.setId(id);
				c.setIdFase(idFase);
				c.setIdTipoProcedimento(idTipoProcedimento);
				c.setIdNatureza(idNatureza);
				c.setCriminal(criminal);
				this.classes.put(id, c);
			}
		} catch (Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de Classes Judiciais: %s", e.getLocalizedMessage()));
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void loadAssuntos() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select id, criminal from assunto");
			while(rs.next()) {
				Integer id = rs.getInt("id");
				Boolean criminal = rs.getBoolean("criminal");
				
				Assunto a = new Assunto();
				a.setId(id);
				a.setCriminal(criminal);
				this.assuntos.put(id, a);
			}
		} catch (Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de Assuntos Judiciais: %s", e.getLocalizedMessage()));
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void loadGraus() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select sigla, id from grau_jurisdicao");
			while(rs.next()) {
				this.graus.put(rs.getString("sigla"), rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de graus: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadTribunais() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select sigla, id from tribunal");
			while(rs.next()) {
				this.tribunais.put(rs.getString("sigla"), rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de tribunais: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadNiveisSigilo() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id from nivel_sigilo");
			while(rs.next()) {
				this.niveisSigilo.add(rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de níveis de sigilo: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadFormatosProcesso() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id from formato_processo");
			while(rs.next()) {
				this.formatosProcesso.add(rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de formatos do processo: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadSistemas() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id from sistema");
			while(rs.next()) {
				this.sistemas.add(rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de sistemas: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadTempos() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select data, id from tempo");
			while(rs.next()) {
				this.tempos.put(rs.getDate("data"), rs.getInt("id"));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de tempos: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadSituacoes() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select id, nome, id_situacao_iniciar, id_situacao_finalizar, tipo_fim_vigencia, inicializacao_condicional, id_fase_processual, id_tipo_procedimento, id_natureza_procedimento, criminal, finaliza_fase_atual from situacao");
			while(rs.next()) {
				Integer id = rs.getInt("id");
				String nome = rs.getString("nome");
				Integer[] iniciar = (Integer[])rs.getArray("id_situacao_iniciar").getArray();
				Integer[] finalizar = (Integer[])rs.getArray("id_situacao_finalizar").getArray();
				String tipoFinalizacao = rs.getString("tipo_fim_vigencia");
				Boolean inicializacaoCondicional = rs.getBoolean("inicializacao_condicional");
				Integer idFase = rs.getInt("id_fase_processual");
				Integer idTipoProcedimento = rs.getInt("id_tipo_procedimento");
				Integer idNatureza = rs.getInt("id_natureza_procedimento");
				Boolean criminal = rs.getBoolean("criminal");
				Boolean finalizaFaseAtual = rs.getBoolean("finaliza_fase_atual");
				
				Situacao s = new Situacao();
				s.setId(id);
				s.setNome(nome);
				s.setIniciar(iniciar);
				s.setFinalizar(finalizar);
				s.setTipoFinalizacao(tipoFinalizacao);
				s.setInicializacaoCondicional(inicializacaoCondicional);
				s.setIdFase(idFase);
				s.setIdTipoProcedimento(idTipoProcedimento);
				s.setIdNatureza(idNatureza);
				s.setCriminal(criminal);
				s.setFinalizaFaseAtual(finalizaFaseAtual);
				
				this.situacoes.put(id, s);
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de situações: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadSituacoesMovimentos() {
		Connection con = getConnection();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs= stmt.executeQuery("select m.id, sm.id_situacao, sm.complemento from situacao_movimento sm inner join movimento m on sm.id_movimento = m.id");
			while(rs.next()) {
				Pair<Integer, String> key = new ImmutablePair<Integer, String>(rs.getInt("id"), rs.getString("complemento"));
				Integer value = rs.getInt("id_situacao");
				this.situacoesMovimentos.put(key, value);
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de situações/movimentos: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isFormatoProcessoValido(Integer id) {
		return formatosProcesso.contains(id);
	}

	public boolean isSistemaValido(Integer id) {
		return sistemas.contains(id);
	}
	
	public boolean isNivelSigiloValido(Integer id) {
		return niveisSigilo.contains(id);
	}
	
	public Integer getIdGrau(String sigla) {
		Integer id = graus.get(sigla);
		return id;
	}
	
	public Integer getIdTribunal(String sigla) {
		Integer id = tribunais.get(sigla);
		return id;
	}
	
	public Situacao getSituacao(Integer id) {
		return situacoes.get(id);
	}
	
	public Integer getIdTempo(String data) {
		String ds = data.replace("-", "").replace("/", "");
		Date d = Misc.stringToDate(ds.substring(0, 8), "yyyyMMdd");
		return getIdTempo(d);
	}
	
	public Integer getIdTempo(Date data) {
		Integer id = tempos.get(data);
		if(id == null) {
			id = selectIdTempo(data);
		}
		return tempos.get(data);
	}
	
	private Integer selectIdTempo(Date data) {
		Integer id =  null;
		Connection con = getConnection();
		try {
			String q = "select id from tempo where data = ?";
			PreparedStatement stmt = con.prepareStatement(q);
			stmt.setDate(1, new java.sql.Date(data.getTime()));
			ResultSet rs= stmt.executeQuery();
			if(rs.next()) {
				id = rs.getInt("id");
				tempos.put(data, id);
			}
			else {
				id = insertTempo(data);
			}
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao carregar cache de tempos: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return id;
	}
	
	private Integer insertTempo(Date data) {
		Integer id =  null;
		Connection con = getConnection();
		try {
			String q = "insert into tempo (data) values (?)";
			PreparedStatement stmt = con.prepareStatement(q);
			stmt.setDate(1, new java.sql.Date(data.getTime()));
			stmt.execute();
			id = selectIdTempo(data);
		}
		catch(Exception e) {
			throw new RuntimeException(String.format("Erro ao inserir tempo: %s", e.getLocalizedMessage()));
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return id;
	}
	
	public OrgaoJulgador getOrgaoJulgadorByName(Integer idTribunal, String name) {
		Optional<OrgaoJulgador> orgaoJulgador = orgaosJulgadores.entrySet().stream().filter(c -> (c.getValue().getId_tribunal() != null && c.getValue().getId_tribunal().equals(idTribunal) && c.getValue().getNome().equals(name))).map(Map.Entry::getValue).findFirst();
		return orgaoJulgador.isPresent() ? orgaoJulgador.get() : null;
	}
	
	public OrgaoJulgador getOrgaoJulgadorById(Integer id) {
		Optional<OrgaoJulgador> orgaoJulgador = orgaosJulgadores.entrySet().stream().filter(c -> c.getValue().getId().equals(id)).map(Map.Entry::getValue).findFirst();
		return orgaoJulgador.isPresent() ? orgaoJulgador.get() : null;
	}
	
	public Classe getClasseById(Integer id) {
		Optional<Classe> classe = classes.entrySet().stream().filter(c -> c.getValue().getId().equals(id)).map(Map.Entry::getValue).findFirst();
		return classe.isPresent() ? classe.get() : null;
	}
	
	public Assunto getAssuntoById(Integer id) {
		Optional<Assunto> assunto = assuntos.entrySet().stream().filter(a -> a.getValue().getId().equals(id)).map(Map.Entry::getValue).findFirst();
		return assunto.isPresent() ? assunto.get() : null;
	}
	
	public Integer getIdAssunto(Integer codigo) {
		Integer id= null;
		Assunto a = getAssuntoById(codigo);
		if(a != null) {
			id = a.getId();
		}
		return id;
	}
	
	public List<Situacao> getSituacoes() {
		return new ArrayList<Situacao>(situacoes.values());
	}
	
	public Integer getIdMovimento(Integer codigo) {
		Integer id = movimentos.contains(codigo)?codigo:null;
		return id;
	}
	
	public String getIdPrioridade(String codigo) {
		String id = prioridades.contains(codigo)?codigo:null;
		return id;
	}
	
	public Integer getIdClasse(Integer codigo) {
		Integer id= null;
		Classe a = getClasseById(codigo);
		if(a != null) {
			id = a.getId();
		}
		return id;
	}
	
	public OrgaoJulgador getOrgaoJulgador(Integer idOrgaoJulgadorOrigem, Integer idTribunal) {
		OrgaoJulgador oj = orgaosJulgadores.get(idOrgaoJulgadorOrigem);
        if (oj != null) {
        	if(idOrgaoJulgadorOrigem != -1 && idOrgaoJulgadorOrigem != 0) {
	    		if(!oj.getId_tribunal().equals(idTribunal)) {
	    			if(idTribunal == 5 && oj.getId_tribunal() == 95) {
	    				oj = getOrgaoJulgadorByName(5, oj.getNome());
	    			}else {
	    				oj = null;
	    			}
	    		}
        	}
        }
        return oj;
    }
	
	public String getCpfMagistrado(String cpf) {
	    String valor = magistrados.contains(cpf)?cpf:null;
	    return valor;
	}
	
	public Integer getIdSituacao(Integer idMovimento, String complemento) {
		Integer id = null;
		if(complemento == null || complemento.trim().length() == 0) {
			Pair<Integer, String> key = new ImmutablePair<Integer, String>(idMovimento, "");
			id = situacoesMovimentos.get(key);
		}
		else {
			id = getIdSituacaoComComplemento(idMovimento, complemento);
			if(id == null) {
				id = getIdSituacaoSemComplemento(idMovimento);
			}
		}
		return id;
	}
	
	private Integer getIdSituacaoComComplemento(Integer idMovimento, String complemento) {
		Integer id = null;
		List<Pair<Integer, String>> keys = new ArrayList<Pair<Integer, String>>(situacoesMovimentos.keySet());
		for (Pair<Integer, String> key : keys) {
			boolean found = false;
			boolean verify = false;
			Integer idMov = key.getKey();
			String comp = key.getValue();
			if(idMov.equals(idMovimento)) {
				found = true;
				if(!Misc.isEmpty(comp)) {
					String[] compArray = comp.split(";");
					for(int i = 0; i<compArray.length; i++) {
						String cod = compArray[i].split(":")[0];
						String val = compArray[i].split(":")[2];
						Matcher m = Pattern.compile(String.format("(%s)\\:([\\w\\h]+)\\:(%s)", cod, val)).matcher(complemento);
        				if(!m.find()) {
        					found = false;
        					break;
        				}else {
        					if(complemento.contains(";")) {
        						String[] cArray = complemento.split(";");
        						for(int j = 0; j<cArray.length; j++) {
        							if(cArray[j].equals(compArray[i])) {
        								verify = true;
        							}
        						}
        					}else {
        						if(complemento.equals(compArray[i])) {
    								verify = true;
    							}
        					}
        				}
					}
					if(found && verify) {
						id = situacoesMovimentos.get(key);
						break;
					}
				}
			}
		}
		return id;
	}
	
	private Integer getIdSituacaoSemComplemento(Integer idMovimento) {
		Integer id = null;
		List<Pair<Integer, String>> keys = new ArrayList<Pair<Integer, String>>(situacoesMovimentos.keySet());
		for (Pair<Integer, String> key : keys) {
			Integer idMov = key.getKey();
			String comp = key.getValue();
			if(idMov.equals(idMovimento) && Misc.isEmpty(comp)) {
				id = situacoesMovimentos.get(key);
				break;
			}
		}
		return id;
	}
	
	/**
	 * Método que converte movimentações em indicadores
	 * @param movimentacoes Lista de movimentações a serem convertidas
	 * @return
	 */
	public List<ProcessoIndicador> converterMovimentacoes(List<Movimentacao> movimentacoes) {
        List<ProcessoIndicador> indicadores = new ArrayList<>();
        List<Integer> situacoesPermitidas = new ArrayList<>();
        int recurso = 0;
        boolean finalizouRecurso = false;
        //Apenas as situações que são mapeadas em indicadores
        Integer[] situacoesLista = new Integer[]{88, 18, 27, 28, 29, 62, 72, 90, 129, 45, 46, 47, 48, 49, 92, 93, 94, 95, 96, 128, 144, 10, 23, 41, 2, 20, 82, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 4, 6, 8, 44, 12, 66, 67, 68, 69, 3, 9, 14, 15, 16, 17, 19, 30, 31, 32, 34, 138, 139, 141, 142, 143, 33, 89, 21, 25, 5, 7, 43, 73, 76, 77, 80, 83, 87, 140, 39};
        situacoesPermitidas.addAll(Arrays.asList(situacoesLista));
        Collections.sort(movimentacoes);
        for (Movimentacao m : movimentacoes) {
        	//Valida apenas se a movimentação não estiver cancelada
        	if(!m.getCancelado()) {
	            SituacaoJson sj = new SituacaoJson(1, m.getIdSituacao(), m.getIdSituacaoIniciar(), m.getIdSituacaoFinalizar(), m.getId(), m.getIdMovimento(),
	                m.getIdClasse(), m.getIdOrgaoJulgador(), m.getIdOrgaoJulgadorColegiado(), m.getCpfMagistrado(), m.getDataInicioSituacao(), m.getDataFimSituacao(), m.getIdAssunto());
	            ProcessoIndicador pij = new ProcessoIndicador(m.getIdProcesso(), m.getIdTribunal(), m.getIdGrau(), m.getIdFase(), m.getIdTipoProcedimento(),
	                getTipo(m.getIdSituacao(), m.getIdSituacaoIniciar()), m.getDataInicioSituacao(), m.getDataInicioSituacao(), m.getDataFimSituacao(), sj, recurso);
	            //Ao ser identificado um tipo de indicador válido
	            if (pij.getIdTipo() > 0) {
	            	//Verificação de finalização de recurso
	            	if((m.getIdGrau() == 2 || m.getIdGrau() == 5) && (pij.getIdTipo() == TIPO_ARQUIVAMENTO || pij.getIdTipo() == TIPO_BAIXA)) {
	            		finalizouRecurso = true;
	            	}
	            	//Verificação de novo Recurso
	            	if(finalizouRecurso && (m.getIdGrau() == 2 || m.getIdGrau() == 5) && (pij.getIdTipo() == TIPO_NOVO || pij.getIdTipo() == TIPO_PENDENTE_LIQUIDO || pij.getIdTipo() == TIPO_REATIVACAO)) {
	            		if(pij.getIdTipo() != TIPO_REATIVACAO) {
		            		recurso++;
		            		pij.setRecurso(pij.getRecurso()+1);
	            		}
		            	finalizouRecurso = false;
	            	}
	            	//Caso o indicador não existir adicona a lista, do contrário atualiza o conteúdo do já existente
	                if (!indicadores.contains(pij)) {
	                    indicadores.add(pij);
	                } else {
	                    indicadores.get(indicadores.indexOf(pij)).updateDados(sj);
	                }
	            }
        	}
        }
        return indicadores;
    }

	/**
	 * Método que identifica o tipo de indicador
	 * @param idSituacao Situação apontada pela movimentação
	 * @param idSituacaoIniciar Situação que gerou a movimentação artificial, quando a mesma existir
	 * @return
	 */
	public Integer getTipo(Integer idSituacao, Integer idSituacaoIniciar){
        if(Arrays.asList(situacoesBaixa).contains(idSituacao)){
            return TIPO_BAIXA;
        }else{
            if(Arrays.asList(situacoesJulgamento).contains(idSituacao)){
                return TIPO_JULGAMENTO;
            }else{
                if(Arrays.asList(situacoesArquivamento).contains(idSituacao)){
                    return TIPO_ARQUIVAMENTO;
                }else{
                    if(Arrays.asList(situacoesSuspensao).contains(idSituacao)){
                        return TIPO_SUSPENSAO;
                    }else{
                        if(Arrays.asList(situacoesDessobrestamento).contains(idSituacao)){
                            return TIPO_DESSOBRESTAMENTO;
                        }else{
                            if(Arrays.asList(situacoesPendente).contains(idSituacao)){
                                if(idSituacaoIniciar != null){
                                	if(Arrays.asList(situacoesIniciarReativacao).contains(idSituacaoIniciar)){
                                        return TIPO_REATIVACAO;
                                    }else{
                                    	if(Arrays.asList(situacoesIniciarNovo).contains(idSituacaoIniciar)){
                                    		return TIPO_NOVO;
                                    	}else {
                                    		if(Arrays.asList(situacoesIniciarRedistribuido).contains(idSituacaoIniciar)){
                                        		return TIPO_REDISTRIBUIDO;
                                        	}else {
                                        		return TIPO_PENDENTE;
                                        	}
                                    	}
                                    }
                                }else{
                                    return TIPO_PENDENTE;
                                }                                    
                            }else{
                                if(Arrays.asList(situacoesArquivamentoProvisorio).contains(idSituacao)){
                                    return TIPO_ARQUIVAMENTO_PROVISORIO;
                                }else{
                                    if(Arrays.asList(situacoesDecisao).contains(idSituacao)){
                                        return TIPO_DECISAO;
                                    }else{
                                        if(Arrays.asList(situacoesAudienciaRealizada).contains(idSituacao)){
                                            return TIPO_AUDIENCIA_REALIZADA;
                                        }else{
                                            if(Arrays.asList(situacoesLiminar).contains(idSituacao)){
                                                return TIPO_LIMINAR;
                                            }else{
                                                if(Arrays.asList(situacoesConcluso).contains(idSituacao)){
                                                    return TIPO_CONCLUSO;
                                                }else{
                                                    if(Arrays.asList(situacoesDespacho).contains(idSituacao)){
                                                        return TIPO_DESPACHO;
                                                    }else{
                                                    	if(Arrays.asList(situacoesPendenteLiquido).contains(idSituacao)){
                                                            return TIPO_PENDENTE_LIQUIDO;
                                                        }else{
                                                        	if(Arrays.asList(situacoesAudienciaRedesignada).contains(idSituacao)){
                                                                return TIPO_AUDIENCIA_REDESIGNADA;
                                                            }else{
                                                            	if(Arrays.asList(situacoesProcedimentoResolvido).contains(idSituacao)){
                                                                    return TIPO_PROCEDIMENTO_RESOLVIDO;
                                                                }else{
                                                                	if(Arrays.asList(situacoesRecursoInterno).contains(idSituacao)){
                                                                        return TIPO_RECURSO_INTERNO;
                                                                    }else{
                                                                    	return TIPO_NAO_INFORMADO;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
	
	/**
	 * Método que verifica se um processo com o mesmo número e tribunal já existem na base do data mart e correspondem as regras de escopo (baixado ou tramitando após 2020) em qualquer grau  
	 * @param idTribunal Tribunal informado
	 * @param numero Número do processo
	 * @return Se o processo está dentro do escopo de migração
	 */
	public boolean getProcessoDentroEscopo(Integer idTribunal, String numero, long idProcesso) {
		Connection con = getConnection();
		boolean exist = false;
        try {
            PreparedStatement ps = con.prepareStatement("select count(1) existe from processo_0"+(idTribunal<10?"0"+idTribunal:idTribunal)+" p where numero = ? and p.flg_fora_recorte = false and p.data_situacao_atual > '2020-01-01' and p.id != ? ");
            ps.setString(1, numero);
            ps.setLong(2, idProcesso);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                exist = rs.getInt("existe") > 0;
            }
        } catch (Exception e) {

        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return exist;
	}
}