package bll;

import dal.AvaliacaoDAL;
import dal.EstudanteDAL;
import dal.InscricaoDAL;
import dal.UcDAL;
import model.Avaliacao;
import model.Estudante;
import model.UnidadeCurricular;
import utils.ExportadorCSV;
import utils.SegurancaPasswords;

import java.util.List;

/**
 * Camada de Lógica de Negócio para o perfil Estudante.
 * Gere pagamentos, atualizações de perfil e segurança de credenciais.
 */
public class EstudanteBLL {

    private static final String PASTA_BD = "bd";

    public Estudante obterPerfilCompleto(String email, String hash) {
        Estudante e = EstudanteDAL.carregarPerfil(email, hash, PASTA_BD);
        if (e == null) return null;

        carregarInscricoes(e);

        List<String[]> dadosNotas = AvaliacaoDAL.obterAvaliacoesPorAluno(e.getNumeroMecanografico(), PASTA_BD);
        UcBLL ucBLL = new UcBLL();

        for (String[] dados : dadosNotas) {
            try {
                String siglaUC = dados[1].trim();
                int anoLetivo = Integer.parseInt(dados[2].trim());

                UnidadeCurricular uc = ucBLL.procurarUCCompleta(siglaUC);

                if (uc != null) {
                    Avaliacao av = new Avaliacao(uc, anoLetivo);
                    if (!dados[3].trim().isEmpty()) av.adicionarResultado(Double.parseDouble(dados[3].trim()));
                    if (dados.length > 4 && !dados[4].trim().isEmpty()) av.adicionarResultado(Double.parseDouble(dados[4].trim()));
                    if (dados.length > 5 && !dados[5].trim().isEmpty()) av.adicionarResultado(Double.parseDouble(dados[5].trim()));

                    e.getPercurso().registarAvaliacao(av);
                }
            } catch (NumberFormatException ex) {
                System.err.println(">> Erro a hidratar nota para o aluno " + e.getNumeroMecanografico());
            }
        }

        return e;
    }

    /**
     * Processa a atualização da morada e grava no ficheiro.
     */
    public void atualizarMorada(Estudante estudante, String novaMorada) {
        estudante.setMorada(novaMorada);
        ExportadorCSV.atualizarEstudante(estudante, PASTA_BD);
    }

    /**
     * Aplica hashing à nova password e atualiza o sistema de credenciais.
     */
    public void alterarPassword(Estudante estudante, String novaPass) {
        String passSegura = SegurancaPasswords.gerarCredencialMista(novaPass);
        estudante.setPassword(passSegura);
        ExportadorCSV.atualizarPasswordCentralizada(estudante.getEmail(), passSegura, PASTA_BD);
    }

    /**
     * Processa um pagamento, atualiza o saldo do objeto e persiste no CSV.
     * @return true se o pagamento foi processado.
     */
    public boolean processarPagamento(Estudante estudante, double valor) {
        if (valor <= 0 || valor > estudante.getSaldoDevedor()) {
            return false;
        }
        estudante.efetuarPagamento(valor);
        ExportadorCSV.atualizarEstudante(estudante, PASTA_BD);
        return true;
    }

    /**
     * Orquestra a hidratação das UCs no percurso do estudante.
     */
    private void carregarInscricoes(Estudante e) {
        List<String> siglasInscritas = InscricaoDAL.obterSiglasUcsPorAluno(e.getNumeroMecanografico(), PASTA_BD);

        for (String sigla : siglasInscritas) {
            UnidadeCurricular uc = UcDAL.procurarUC(sigla, PASTA_BD);
            if (uc != null) {
                e.getPercurso().inscreverEmUc(uc);
            }
        }
    }
}