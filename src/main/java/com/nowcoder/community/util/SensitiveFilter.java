package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
@Component
public class SensitiveFilter {
    private static final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    //根节点
    private TrieNode rootNode=new TrieNode();

    //前缀树
    private class TrieNode{
        // 关键词结束标识
        private boolean isKeywordEnd = false;
        //子节点(key是下级字符,value是下级节点)
        private Map<Character,TrieNode> subNodes=new HashMap<>();

        public boolean isKeywordEnd(){
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean KeywordEnd){
            isKeywordEnd=KeywordEnd;
        }

        public void addSubNodes(Character c,TrieNode node){
            subNodes.put(c,node);
        }

        public TrieNode getSubNodes(Character c){
            return subNodes.get(c);
        }

    }

    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
    }

    //将敏感词添加到前缀树
    private void addKeyword(String keyword) {
        TrieNode tempNode=rootNode;
        for (int i=0; i<keyword.length(); i++){
            char c=keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNodes(c);
            if(subNode==null){
                subNode = new TrieNode();
                tempNode.addSubNodes(c,subNode);
            }
            tempNode=subNode;
            //设置结束标志
            if(i==keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();
        while(begin<text.length()){
            char c=text.charAt(position);
            if(isSymbol(c)){
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if(tempNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }
            tempNode=tempNode.getSubNodes(c);
            if(tempNode==null){
                sb.append(text.charAt(begin));
                position=++begin;
                tempNode=rootNode;
            }else if(tempNode.isKeywordEnd()){
                sb.append(REPLACEMENT);
                begin=++position;
                tempNode=rootNode;
            }else{
                if(position<text.length()-1){
                    position++;
                }else{
                    position=begin;
                }
            }
        }
        return sb.toString();
    }
}
