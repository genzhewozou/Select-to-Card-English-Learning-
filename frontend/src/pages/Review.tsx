import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Button, message, Slider, Space } from 'antd';
import { getTodayReviewCards, getWeakCards, submitReview } from '../services/reviewService';
import type { CardDTO } from '../types/api';

/**
 * 学习复习页：展示今日待复习或错题本卡片；
 * 支持 URL ?mode=weak 只复习错题（熟练度 1-2）。
 */
export default function Review() {
  const [searchParams] = useSearchParams();
  const modeWeak = searchParams.get('mode') === 'weak';
  const [list, setList] = useState<CardDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [proficiency, setProficiency] = useState(3);
  const [submitting, setSubmitting] = useState(false);
  const [showBack, setShowBack] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const data = modeWeak ? await getWeakCards() : await getTodayReviewCards();
      setList(data ?? []);
      setCurrentIndex(0);
      setShowBack(false);
      setProficiency(3);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [modeWeak]);

  const current = list[currentIndex];

  const handleSubmitReview = async () => {
    if (!current?.id) return;
    setSubmitting(true);
    try {
      await submitReview({ cardId: current.id, proficiencyLevel: proficiency });
      message.success('已记录，下次将按艾宾浩斯时间推荐复习');
      const nextList = list.filter((_, i) => i !== currentIndex);
      setList(nextList);
      setCurrentIndex(currentIndex >= list.length - 1 ? 0 : currentIndex);
      setShowBack(false);
      setProficiency(3);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div style={{ padding: 24 }}>加载中...</div>;
  }

  if (list.length === 0) {
    return (
      <Card>
        <h2>{modeWeak ? '错题复习' : '今日复习'}</h2>
        <p>
          {modeWeak
            ? '暂无熟练度 1～2 级的卡片。在「今日复习」中标记不熟后，会出现在错题本。'
            : '今日没有需要复习的卡片，或你尚未添加任何学习进度。去文档中选词生成卡片并复习一次后，这里会显示待复习列表。'}
        </p>
      </Card>
    );
  }

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>
        {modeWeak ? '错题复习' : '今日复习'}（{currentIndex + 1} / {list.length}）
      </h2>
      <Card>
        <p style={{ marginBottom: 8, color: '#666' }}>正面：</p>
        <p style={{ fontSize: 18, marginBottom: 16 }}>{current?.frontContent}</p>
        {!showBack ? (
          <Button type="primary" onClick={() => setShowBack(true)}>
            显示背面
          </Button>
        ) : (
          <>
            <p style={{ marginBottom: 8, color: '#666' }}>背面：</p>
            <p style={{ fontSize: 16, marginBottom: 16 }}>{current?.backContent || '（无）'}</p>
            <p style={{ marginBottom: 8 }}>熟练度（1-5）：</p>
            <Slider
              min={1}
              max={5}
              value={proficiency}
              onChange={setProficiency}
              marks={{ 1: '1', 2: '2', 3: '3', 4: '4', 5: '5' }}
              style={{ marginBottom: 24 }}
            />
            <Space>
              <Button type="primary" loading={submitting} onClick={handleSubmitReview}>
                提交并下一张
              </Button>
            </Space>
          </>
        )}
      </Card>
    </div>
  );
}
