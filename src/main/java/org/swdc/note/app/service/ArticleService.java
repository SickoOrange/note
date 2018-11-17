package org.swdc.note.app.service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swdc.note.app.entity.Article;
import org.swdc.note.app.entity.ArticleContext;
import org.swdc.note.app.entity.ArticleType;
import org.swdc.note.app.repository.ArticleRepository;
import org.swdc.note.app.repository.ArticleTypeRepository;
import org.swdc.note.app.ui.UIConfig;
import org.swdc.note.app.util.DataUtil;

import java.util.List;
import java.util.Map;

/**
 * 文章服务，提供关于文章的各种操作
 */
@Service
public class ArticleService {

    @Autowired
    private Parser parser;

    @Autowired
    private HtmlRenderer renderer;

    @Autowired
    private UIConfig config;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleTypeRepository typeRepository;

    @Transactional
    public List<Article> loadArticles(ArticleType type){
        type = typeRepository.getOne(type.getId());
        Hibernate.initialize(type.getArticles());
        return type.getArticles();
    }

    @Transactional
    public ArticleContext loadContext(Article article){
        article = articleRepository.getOne(article.getId());
        Hibernate.initialize(article.getContext());
        return article.getContext();
    }

    @Transactional
    public Article saveArticle(Article article, ArticleContext context){
        if(article.getId() == null){
            article.setContext(context);
            return articleRepository.save(article);
        }
        Article articleOld = articleRepository.getOne(article.getId());
        Hibernate.initialize(articleOld.getContext());
        ArticleContext contextOld = articleOld.getContext();
        // 更新持久态对象
        contextOld = DataUtil.updateProperties(context,contextOld);
        contextOld.setImageRes(context.getImageRes());
        articleOld = DataUtil.updateProperties(article, articleOld);
        articleOld.setContext(contextOld);
        articleOld.setType(typeRepository.getOne(article.getType().getId()));

        return articleRepository.save(articleOld);
    }

    @Transactional
    public void deleteArticle(Article article){
        article = articleRepository.getOne(article.getId());
        articleRepository.delete(article);
    }

    @Transactional
    public List<Article> searchArticleByTitle(String key){
        return articleRepository.findByTitleContaining(key);
    }

    public String compile(ArticleContext context){
        Map<String,String> resource = context.getImageRes();
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n");
        resource.entrySet().forEach(ent->
                sb.append("[")
                        .append(ent.getKey())
                        .append("]: data:image/png;base64,")
                        .append(ent.getValue())
                        .append("\n"));
        String content = renderer.render(parser.parse(context.getContent()+"\n"+sb.toString()));
        content = "<!doctype html><html><head><style>"+config.getMdStyleContent()+"</style></head>"
                +"<body ondragstart='return false;'>"+content+"</body></html>";
        return content;
    }

}
