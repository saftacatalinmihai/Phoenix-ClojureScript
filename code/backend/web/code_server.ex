defmodule Backend.CodeServer do
  @moduledoc false
  
  use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, name: __MODULE__)
  end

  def get_actor_types do
    GenServer.call(__MODULE__, :get_actor_types)
  end
  
  def new_actor_type(actor_type) do
    GenServer.call(__MODULE__, {:new_actor_type, actor_type})
  end

  def start_actor(actor_type) do
    GenServer.call(__MODULE__, {:start_actor, actor_type})
  end

  def get_actor_code(actor_type) do
    GenServer.call(__MODULE__, {:get_code, actor_type})
  end

  def update_actor_code(actor_type, new_code) do
    GenServer.call(__MODULE__, {:update_code, actor_type, new_code})
  end

  def init(:ok) do
    actor_types = getActors() |> evalActors
    {:ok, %{:actor_types => actor_types}}
  end

  def handle_call(:get_actor_types, _from, state) do
    {:reply, state[:actor_types], state}
  end

  def handle_call({:start_actor, actor_type}, _from, state) do
    case :"Elixir.#{actor_type}".start_link do
      {:ok, pid} -> 
        IO.inspect pid
        IO.inspect :erlang.trace(pid, true, [:send, :receive, :exiting, :timestamp, {:tracer, Backend.EventStore.get_pid()}])
        {:reply, {:ok, %{:name => actor_type, :pid => to_string(:erlang.pid_to_list(pid))}}, state}
      _ ->
        {:reply, {:error, %{:reason => "Unable to start actor type: #{actor_type}"}}, state}
    end
  end

  def handle_call({:new_actor_type, actor_type}, _from, %{:actor_types => actor_types}) do
    if not Enum.member?(actor_types, actor_type) do
      case File.read("code/template/actor.ex") do
        {:ok, body} ->
          {:ok, file} = File.open "code/#{actor_type}.ex", [:write]
          IO.binwrite file, String.replace(body, "{{actor_name}}", actor_type)
          Code.eval_file("code/#{actor_type}.ex")
          {:reply, {:ok, %{:actor_type => actor_type}}, %{:actor_types => [actor_type | actor_types]}}
        {:error, reason} ->
          IO.inspect reason
          {:reply, {:error, %{:reason => reason}}, %{:actor_types => actor_types}}
      end
    else
      {:reply, {:ok, %{:actor_type => actor_type}}, %{:actor_types => actor_types}}
    end
  end
  
  def handle_call({:get_code, actor_type}, _from, state) do
    case File.read("code/#{actor_type}.ex") do
      {:ok, body} ->
        {:reply, {:ok, %{:code => body}}, state}
      {:error, reason} ->
        {:reply, {:error, %{:reason => reason}}, state}
    end
  end

  def handle_call({:update_code, actor_type, new_code}, _from, state) do
    {:ok, file} = File.open "code/#{actor_type}.ex", [:write]
    IO.binwrite file, new_code
    try do
      IO.inspect Code.eval_file("code/#{actor_type}.ex")
      {:reply, {:ok, %{:name => actor_type}}, state}
    rescue
      e ->
        IO.inspect e
        {:reply, {:error, %{:reson => e}}, state}
    end
  end

  def handle_call(_msg, _from, state) do
    {:reply, :ok, state}
  end

  def handle_cast(_msg, state) do
    {:noreply, state}
  end

  defp getActors do
    Path.wildcard("code/*.ex") |>
      Enum.map(fn file ->
        String.replace_prefix(file, "code/", "") |>
          String.replace_suffix(".ex", "")
      end)
  end

  defp evalActors(actors) do
    actors |> Enum.map(&evalActor/1)
  end

  defp evalActor(type) do
    Code.eval_file("code/#{type}.ex")
    type
  end

end
