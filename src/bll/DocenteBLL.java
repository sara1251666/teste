package bll;

import dal.AvaliacaoDAL;
import dal.CredencialDAL;
import dal.EstudanteDAL;
import dal.UcDAL;
import model.Avaliacao;
import model.Docente;
import model.Estudante;
import model.UnidadeCurricular;
import utils.SegurancaPasswords;

import java.util.ArrayList;
import java.util.List;


/**
 * Lógica de negócio para o perfil Docente.
 * Gere o lançamento de avaliações com todas as validações necessárias,
 * a obtenção dos alunos associados e a alteração segura de credenciais.
 */
public class DocenteBLL {

    private static final String PASTA_BD = "bd";


    /**
     * Verifica se uma UC pertence ao plano de lecionação do docente.
     * @param docente Docente autenticado.
     * @param siglaUc Sigla da UC a verificar.
     * @return true se o docente lecionar a UC indicada.
     */
    public boolean lecionaEstaUC(Docente docente, String siglaUc) {
        if (siglaUc == null) return false;
        for (int i = 0; i < docente.getTotalUcsLecionadas(); i++) {
            if (docente.getUcsLecionadas()[i].getSigla().equalsIgnoreCase(siglaUc)) return true;
        }
        return false;
    }

    /**
     * Regista uma avaliação para um aluno numa UC após validação completa.
     * Valida se o aluno existe, se a UC pertence ao docente
     * e se ainda não existe avaliação para o mesmo aluno/UC/ano.
     * @param numMec  Número mecanográfico do aluno.
     * @param siglaUc Sigla da UC avaliada.
     * @param ano     Ano letivo da avaliação.
     * @param n1      Nota do 1.º momento (-1 se não aplicável).
     * @param n2      Nota do 2.º momento (-1 se não aplicável).
     * @param n3      Nota do 3.º momento (-1 se não aplicável).
     * @param d       Docente que lança a avaliação.
     * @return null se a operação foi bem-sucedida; mensagem de erro caso contrário.
     */
    public String lancarNota(int numMec, String siglaUc, int ano,
                             double n1, double n2, double n3, Docente d) {
        Estudante aluno = EstudanteDAL.procurarPorNumMec(numMec, PASTA_BD);
        if (aluno == null)
            return "ERRO: Aluno com nº " + numMec + " não encontrado.";

        if (!lecionaEstaUC(d, siglaUc))
            return "ERRO: A UC '" + siglaUc + "' não pertence às suas unidades curriculares.";

        if (AvaliacaoDAL.existeAvaliacao(numMec, siglaUc, ano, PASTA_BD))
            return "ERRO: Já existe uma avaliação registada para o aluno " + numMec
                    + " na UC '" + siglaUc + "' no ano " + ano + ".";

        UnidadeCurricular uc = new UcBLL().procurarUCCompleta(siglaUc);
        if (uc == null)
            return "ERRO: A UC '" + siglaUc + "' não foi encontrada no sistema.";

        Avaliacao aval = new Avaliacao(uc, ano);
        if (n1 >= 0) aval.adicionarResultado(n1);
        if (n2 >= 0) aval.adicionarResultado(n2);
        if (n3 >= 0) aval.adicionarResultado(n3);

        AvaliacaoDAL.adicionarAvaliacao(aval, numMec, PASTA_BD);
        return null;
    }


    /**
     * Altera a password do docente com hashing e persistência.
     * @param docente  Docente autenticado.
     * @param novaPass Nova password em texto limpo.
     */
    public void alterarPassword(Docente docente, String novaPass) {
        String passSegura = SegurancaPasswords.gerarCredencialMista(novaPass);
        docente.setPassword(passSegura);
        CredencialDAL.atualizarPassword(docente.getEmail(), passSegura, PASTA_BD);
    }

    /**
     * Devolve os alunos associados ao docente com a respetiva média global.
     * Cada elemento da lista é um array com [Estudante, Double (média)].
     * @param docente Docente autenticado.
     * @return Lista de pares [Estudante, média].
     */
    public List<Object[]> obterAlunosDoDocenteComMedia(Docente docente) {
        List<Estudante> todos = new EstudanteBLL().carregarTodosCompleto();
        List<Object[]> resultado = new ArrayList<>();

        if (todos == null) return resultado;

        for (Estudante e : todos) {
            if (e == null || e.getPercurso() == null) continue;

            boolean alunoDoDocente = false;
            double somaMedias = 0;
            int totalAvaliacoes = 0;

            for (int i = 0; i < e.getPercurso().getTotalAvaliacoes(); i++) {
                Avaliacao av = e.getPercurso().getHistoricoAvaliacoes()[i];
                if (av == null || av.getUc() == null) continue;

                if (lecionaEstaUC(docente, av.getUc().getSigla())) {
                    alunoDoDocente = true;
                    somaMedias += av.calcularMedia();
                    totalAvaliacoes++;
                }
            }

            if (alunoDoDocente) {
                double media = totalAvaliacoes > 0 ? somaMedias / totalAvaliacoes : 0.0;
                resultado.add(new Object[]{e, media});
            }
        }
        return resultado;
    }
}