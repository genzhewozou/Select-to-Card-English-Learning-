import request from '../utils/request';
import type {
  QuizAnswerResponse,
  QuizResultResponse,
  QuizSessionStartResponse,
  Result,
} from '../types/api';

export function startQuizSession(params: {
  documentId: number;
  questionCount?: number;
  prioritizeWrong?: boolean;
  prioritizeLowProficiency?: boolean;
  useAiTempExamples?: boolean;
  aiApiKey?: string;
  aiModel?: string;
  aiBaseUrl?: string;
}) {
  return request
    .post<Result<QuizSessionStartResponse>>('/quiz/session', params)
    .then((r) => r.data.data);
}

export function submitQuizAnswer(
  sessionId: number,
  itemId: number,
  answer: string,
  answerExtra?: string,
  ai?: { aiApiKey?: string; aiModel?: string; aiBaseUrl?: string },
) {
  return request
    .post<Result<QuizAnswerResponse>>(`/quiz/session/${sessionId}/answer`, {
      itemId,
      answer,
      answerExtra,
      aiApiKey: ai?.aiApiKey,
      aiModel: ai?.aiModel,
      aiBaseUrl: ai?.aiBaseUrl,
    })
    .then((r) => r.data.data);
}

export function getQuizResult(sessionId: number) {
  return request
    .get<Result<QuizResultResponse>>(`/quiz/session/${sessionId}/result`)
    .then((r) => r.data.data);
}

export function getQuizSessionDetail(sessionId: number) {
  return request
    .get<Result<QuizSessionStartResponse>>(`/quiz/session/${sessionId}`)
    .then((r) => r.data.data);
}

export function listQuizSessions() {
  return request
    .get<Result<QuizResultResponse[]>>('/quiz/session/list')
    .then((r) => r.data.data);
}

export function retryQuizWrong(sessionId: number) {
  return request
    .post<Result<QuizSessionStartResponse>>(`/quiz/session/${sessionId}/retry-wrong`)
    .then((r) => r.data.data);
}
